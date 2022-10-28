package com.mojin.qidon;
import com.sd.lib.switchbutton.SwitchButton;
import android.view.View;

public class SwitchButtons {
    public static void QualitySettings(final SwitchButton LowButton, final SwitchButton MediumButton, final SwitchButton HighButton) {
        LowButton.setOnCheckedChangeCallback(new SwitchButton.OnCheckedChangeCallback()
            {
                @Override
                public void onCheckedChanged(boolean cheacked,SwitchButton switchButton) {
                    boolean checked = LowButton.isChecked();
                    if (checked) {
                        MediumButton.setChecked(false, true, true);
                        HighButton.setChecked(false, true, true);
                    }
                }
            });

        MediumButton.setOnCheckedChangeCallback(new SwitchButton.OnCheckedChangeCallback()
            {
                @Override
                public void onCheckedChanged(boolean cheacked,SwitchButton switchButton) {
                    boolean checked = MediumButton.isChecked();
                    if (checked) {
                        LowButton.setChecked(false, true, true);
                        HighButton.setChecked(false, true, true);
                    }
                }
            });

        HighButton.setOnCheckedChangeCallback(new SwitchButton.OnCheckedChangeCallback()
            {
                @Override
                public void onCheckedChanged(boolean cheacked,SwitchButton switchButton) {
                    boolean checked = HighButton.isChecked();
                    if (checked) {
                        MediumButton.setChecked(false, true, true);
                        LowButton.setChecked(false, true, true);
                    }
                }
            });
    }
    
    public static void GamePackage(SwitchButton SuperGamePackage, SwitchButton StoryGamePackage, SwitchButton ModelGamePackage) {
        final SwitchButton SuperPackage = SuperGamePackage;
        final SwitchButton StoryPackage = StoryGamePackage;
        final SwitchButton ModelPackage = ModelGamePackage;
        SuperPackage.setOnCheckedChangeCallback(new SwitchButton.OnCheckedChangeCallback()
            {
                @Override
                public void onCheckedChanged(boolean cheacked,SwitchButton switchButton) {
                    boolean checked = SuperPackage.isChecked();
                    if (checked) {
                        StoryPackage.setChecked(false, true, true);
                        ModelPackage.setChecked(false, true, true);
                    }
                }
            });

        StoryPackage.setOnCheckedChangeCallback(new SwitchButton.OnCheckedChangeCallback()
            {
                @Override
                public void onCheckedChanged(boolean cheacked,SwitchButton switchButton) {
                    boolean checked = StoryPackage.isChecked();
                    if (checked) {
                        SuperPackage.setChecked(false, true, true);
                        ModelPackage.setChecked(false, true, true);
                    }
                }
            });

        ModelPackage.setOnCheckedChangeCallback(new SwitchButton.OnCheckedChangeCallback()
            {
                @Override
                public void onCheckedChanged(boolean cheacked,SwitchButton switchButton) {
                    boolean checked = ModelPackage.isChecked();
                    if (checked) {
                        StoryPackage.setChecked(false, true, true);
                        SuperPackage.setChecked(false, true, true);
                    }
                }
            });
    }

    public static void SelectServer(final SwitchButton ServerA, final SwitchButton ServerB, final SwitchButton ServerC, final SwitchButton ServerD) {
        ServerA.setOnCheckedChangeCallback(new SwitchButton.OnCheckedChangeCallback()
            {
                @Override
                public void onCheckedChanged(boolean cheacked,SwitchButton switchButton) {
                    boolean checked = ServerA.isChecked();
                    if (checked) {
                        ServerB.setChecked(false, true, true);
                        ServerC.setChecked(false, true, true);
                        ServerD.setChecked(false, true, true);
                    }
                }
            });
            
        ServerB.setOnCheckedChangeCallback(new SwitchButton.OnCheckedChangeCallback()
            {
                @Override
                public void onCheckedChanged(boolean cheacked,SwitchButton switchButton) {
                    boolean checked = ServerB.isChecked();
                    if (checked) {
                        ServerA.setChecked(false, true, true);
                        ServerC.setChecked(false, true, true);
                        ServerD.setChecked(false, true, true);
                    }
                }
            });
        
        ServerC.setOnCheckedChangeCallback(new SwitchButton.OnCheckedChangeCallback()
            {
                @Override
                public void onCheckedChanged(boolean cheacked,SwitchButton switchButton) {
                    boolean checked = ServerC.isChecked();
                    if (checked) {
                        ServerB.setChecked(false, true, true);
                        ServerA.setChecked(false, true, true);
                        ServerD.setChecked(false, true, true);
                    }
                }
            });
        
        ServerD.setOnCheckedChangeCallback(new SwitchButton.OnCheckedChangeCallback()
            {
                @Override
                public void onCheckedChanged(boolean cheacked,SwitchButton switchButton) {
                    boolean checked = ServerD.isChecked();
                    if (checked) {
                        ServerB.setChecked(false, true, true);
                        ServerC.setChecked(false, true, true);
                        ServerA.setChecked(false, true, true);
                    }
                }
            });
    }
}
