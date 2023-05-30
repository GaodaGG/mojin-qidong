#include <mutex>

#include "VulkanFrameData.h"
#include "Common/Log.h"
#include "Common/StringUtils.h"

void CachedReadback::Destroy(VulkanContext *vulkan) {
	if (buffer) {
		vulkan->Delete().QueueDeleteBufferAllocation(buffer, allocation);
	}
	bufferSize = 0;
}

void FrameData::Init(VulkanContext *vulkan, int index) {
	this->index = index;
	VkDevice device = vulkan->GetDevice();

	VkCommandPoolCreateInfo cmd_pool_info = { VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO };
	cmd_pool_info.queueFamilyIndex = vulkan->GetGraphicsQueueFamilyIndex();
	cmd_pool_info.flags = VK_COMMAND_POOL_CREATE_TRANSIENT_BIT;
	VkResult res = vkCreateCommandPool(device, &cmd_pool_info, nullptr, &cmdPoolInit);
	_dbg_assert_(res == VK_SUCCESS);
	res = vkCreateCommandPool(device, &cmd_pool_info, nullptr, &cmdPoolMain);
	_dbg_assert_(res == VK_SUCCESS);

	VkCommandBufferAllocateInfo cmd_alloc = { VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO };
	cmd_alloc.commandPool = cmdPoolInit;
	cmd_alloc.level = VK_COMMAND_BUFFER_LEVEL_PRIMARY;
	cmd_alloc.commandBufferCount = 1;
	res = vkAllocateCommandBuffers(device, &cmd_alloc, &initCmd);
	_dbg_assert_(res == VK_SUCCESS);
	cmd_alloc.commandPool = cmdPoolMain;
	res = vkAllocateCommandBuffers(device, &cmd_alloc, &mainCmd);
	res = vkAllocateCommandBuffers(device, &cmd_alloc, &presentCmd);
	_dbg_assert_(res == VK_SUCCESS);

	vulkan->SetDebugName(initCmd, VK_OBJECT_TYPE_COMMAND_BUFFER, StringFromFormat("initCmd%d", index).c_str());
	vulkan->SetDebugName(mainCmd, VK_OBJECT_TYPE_COMMAND_BUFFER, StringFromFormat("mainCmd%d", index).c_str());
	vulkan->SetDebugName(presentCmd, VK_OBJECT_TYPE_COMMAND_BUFFER, StringFromFormat("presentCmd%d", index).c_str());

	// Creating the frame fence with true so they can be instantly waited on the first frame
	fence = vulkan->CreateFence(true);
	vulkan->SetDebugName(fence, VK_OBJECT_TYPE_FENCE, StringFromFormat("fence%d", index).c_str());
	readyForFence = true;

	VkQueryPoolCreateInfo query_ci{ VK_STRUCTURE_TYPE_QUERY_POOL_CREATE_INFO };
	query_ci.queryCount = MAX_TIMESTAMP_QUERIES;
	query_ci.queryType = VK_QUERY_TYPE_TIMESTAMP;
	res = vkCreateQueryPool(device, &query_ci, nullptr, &profile.queryPool);
}

void FrameData::Destroy(VulkanContext *vulkan) {
	VkDevice device = vulkan->GetDevice();
	vkDestroyCommandPool(device, cmdPoolInit, nullptr);
	vkDestroyCommandPool(device, cmdPoolMain, nullptr);
	vkDestroyFence(device, fence, nullptr);
	vkDestroyQueryPool(device, profile.queryPool, nullptr);

	readbacks_.IterateMut([=](const ReadbackKey &key, CachedReadback *value) {
		value->Destroy(vulkan);
		delete value;
	});
	readbacks_.Clear();
}

void FrameData::AcquireNextImage(VulkanContext *vulkan, FrameDataShared &shared) {
	_dbg_assert_(!hasAcquired);

	// Get the index of the next available swapchain image, and a semaphore to block command buffer execution on.
	VkResult res = vkAcquireNextImageKHR(vulkan->GetDevice(), vulkan->GetSwapchain(), UINT64_MAX, shared.acquireSemaphore, (VkFence)VK_NULL_HANDLE, &curSwapchainImage);
	switch (res) {
	case VK_SUCCESS:
		hasAcquired = true;
		break;
	case VK_SUBOPTIMAL_KHR:
		hasAcquired = true;
		// Hopefully the resize will happen shortly. Ignore - one frame might look bad or something.
		WARN_LOG(G3D, "VK_SUBOPTIMAL_KHR returned - ignoring");
		break;
	case VK_ERROR_OUT_OF_DATE_KHR:
	case VK_TIMEOUT:
	case VK_NOT_READY:
		// We do not set hasAcquired here!
		WARN_LOG(G3D, "%s returned from AcquireNextImage - processing the frame, but not presenting", VulkanResultToString(res));
		skipSwap = true;
		break;
	default:
		// Weird, shouldn't get any other values. Maybe lost device?
		_assert_msg_(false, "vkAcquireNextImageKHR failed! result=%s", VulkanResultToString(res));
		break;
	}
}

VkResult FrameData::QueuePresent(VulkanContext *vulkan, FrameDataShared &shared) {
	_dbg_assert_(hasAcquired);
	hasAcquired = false;
	_dbg_assert_(!skipSwap);

	VkSwapchainKHR swapchain = vulkan->GetSwapchain();
	VkPresentInfoKHR present = { VK_STRUCTURE_TYPE_PRESENT_INFO_KHR };
	present.swapchainCount = 1;
	present.pSwapchains = &swapchain;
	present.pImageIndices = &curSwapchainImage;
	present.pWaitSemaphores = &shared.renderingCompleteSemaphore;
	present.waitSemaphoreCount = 1;

	return vkQueuePresentKHR(vulkan->GetGraphicsQueue(), &present);
}

VkCommandBuffer FrameData::GetInitCmd(VulkanContext *vulkan) {
	if (!hasInitCommands) {
		VkCommandBufferBeginInfo begin = {
			VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO,
			nullptr,
			VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT
		};
		vkResetCommandPool(vulkan->GetDevice(), cmdPoolInit, 0);
		VkResult res = vkBeginCommandBuffer(initCmd, &begin);
		if (res != VK_SUCCESS) {
			return VK_NULL_HANDLE;
		}

		// Good spot to reset the query pool.
		if (profilingEnabled_) {
			vkCmdResetQueryPool(initCmd, profile.queryPool, 0, MAX_TIMESTAMP_QUERIES);
			vkCmdWriteTimestamp(initCmd, VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT, profile.queryPool, 0);
		}

		hasInitCommands = true;
	}
	return initCmd;
}

void FrameData::SubmitPending(VulkanContext *vulkan, FrameSubmitType type, FrameDataShared &sharedData) {
	VkCommandBuffer cmdBufs[3];
	int numCmdBufs = 0;

	VkFence fenceToTrigger = VK_NULL_HANDLE;

	if (hasInitCommands) {
		if (profilingEnabled_) {
			// Pre-allocated query ID 1 - end of init cmdbuf.
			vkCmdWriteTimestamp(initCmd, VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT, profile.queryPool, 1);
		}

		VkResult res = vkEndCommandBuffer(initCmd);
		cmdBufs[numCmdBufs++] = initCmd;

		_assert_msg_(res == VK_SUCCESS, "vkEndCommandBuffer failed (init)! result=%s", VulkanResultToString(res));
		hasInitCommands = false;
	}

	if ((hasMainCommands || hasPresentCommands) && type == FrameSubmitType::Sync) {
		fenceToTrigger = sharedData.readbackFence;
	}

	if (hasMainCommands) {
		VkResult res = vkEndCommandBuffer(mainCmd);
		_assert_msg_(res == VK_SUCCESS, "vkEndCommandBuffer failed (main)! result=%s", VulkanResultToString(res));

		cmdBufs[numCmdBufs++] = mainCmd;
		hasMainCommands = false;
	}

	if (hasPresentCommands && type != FrameSubmitType::Pending) {
		VkResult res = vkEndCommandBuffer(presentCmd);
		_assert_msg_(res == VK_SUCCESS, "vkEndCommandBuffer failed (present)! result=%s", VulkanResultToString(res));

		cmdBufs[numCmdBufs++] = presentCmd;
		hasPresentCommands = false;

		if (type == FrameSubmitType::Present) {
			fenceToTrigger = fence;
		}
	}

	if (!numCmdBufs && fenceToTrigger == VK_NULL_HANDLE) {
		// Nothing to do.
		return;
	}

	VkSubmitInfo submit_info{ VK_STRUCTURE_TYPE_SUBMIT_INFO };
	VkPipelineStageFlags waitStage[1]{ VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT };
	if (type == FrameSubmitType::Present && !skipSwap) {
		_dbg_assert_(hasAcquired);
		submit_info.waitSemaphoreCount = 1;
		submit_info.pWaitSemaphores = &sharedData.acquireSemaphore;
		submit_info.pWaitDstStageMask = waitStage;
	}
	submit_info.commandBufferCount = (uint32_t)numCmdBufs;
	submit_info.pCommandBuffers = cmdBufs;
	if (type == FrameSubmitType::Present && !skipSwap) {
		submit_info.signalSemaphoreCount = 1;
		submit_info.pSignalSemaphores = &sharedData.renderingCompleteSemaphore;
	}

	VkResult res;
	if (fenceToTrigger == fence) {
		// The fence is waited on by the main thread, they are not allowed to access it simultaneously.
		res = vkQueueSubmit(vulkan->GetGraphicsQueue(), 1, &submit_info, fenceToTrigger);
		std::lock_guard<std::mutex> lock(fenceMutex);
		readyForFence = true;
		fenceCondVar.notify_one();
	} else {
		res = vkQueueSubmit(vulkan->GetGraphicsQueue(), 1, &submit_info, fenceToTrigger);
	}

	if (res == VK_ERROR_DEVICE_LOST) {
		_assert_msg_(false, "Lost the Vulkan device in vkQueueSubmit! If this happens again, switch Graphics Backend away from Vulkan");
	} else {
		_assert_msg_(res == VK_SUCCESS, "vkQueueSubmit failed (main)! result=%s", VulkanResultToString(res));
	}

	if (type == FrameSubmitType::Sync) {
		// Hard stall of the GPU, not ideal, but necessary so the CPU has the contents of the readback.
		vkWaitForFences(vulkan->GetDevice(), 1, &sharedData.readbackFence, true, UINT64_MAX);
		vkResetFences(vulkan->GetDevice(), 1, &sharedData.readbackFence);
		syncDone = true;
	}
}

void FrameDataShared::Init(VulkanContext *vulkan) {
	VkSemaphoreCreateInfo semaphoreCreateInfo = { VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO };
	semaphoreCreateInfo.flags = 0;
	VkResult res = vkCreateSemaphore(vulkan->GetDevice(), &semaphoreCreateInfo, nullptr, &acquireSemaphore);
	_dbg_assert_(res == VK_SUCCESS);
	res = vkCreateSemaphore(vulkan->GetDevice(), &semaphoreCreateInfo, nullptr, &renderingCompleteSemaphore);
	_dbg_assert_(res == VK_SUCCESS);

	// This fence is used for synchronizing readbacks. Does not need preinitialization.
	readbackFence = vulkan->CreateFence(false);
	vulkan->SetDebugName(readbackFence, VK_OBJECT_TYPE_FENCE, "readbackFence");
}

void FrameDataShared::Destroy(VulkanContext *vulkan) {
	VkDevice device = vulkan->GetDevice();
	vkDestroySemaphore(device, acquireSemaphore, nullptr);
	vkDestroySemaphore(device, renderingCompleteSemaphore, nullptr);
	vkDestroyFence(device, readbackFence, nullptr);
}
