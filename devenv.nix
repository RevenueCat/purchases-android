{ pkgs, ... }:

{
  languages.java = {
    enable = true;
    jdk.package = pkgs.jdk21;
  };

  android = {
    enable = true;
    platforms.version = [ "35" ];
    buildTools.version = [ "35.0.0" ];
    cmdLineTools.version = "11.0";
    emulator.enable = false;
    systemImages.enable = false;
    sources.enable = false;
    ndk.enable = false;
    googleAPIs.enable = false;
    googleTVAddOns.enable = false;
  };
}
