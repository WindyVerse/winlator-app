<p align="center">
	<img src="logo.png" width="376" height="128" alt="Winlator Logo" />
</p>

# WindyVerse branch

The `windyverse` branch keeps only the parts of Winlator that WindyVerse PC Gamer embeds:
the Java X server and its renderer, the PulseAudio and SysV shared memory servers, and the
touchpad/X server views. The `app` module is an Android library; the host app declares the
Activity and services. Changes on this branch:

- The X server is hosted by any `Context` and emits debug output through `ProcessHelper`.
- Client output is written by one ordered writer per connection (`OrderedOutput`).
- The container UI, Box64/glibc runtime, input controls, WinHandler, GLX, Vortek/VirGL,
  MIDI and their resources, assets and native libraries are removed.

Rebase onto upstream `main` to take X server fixes; build it as part of PC Gamer.

# Winlator

Winlator is an Android application that lets you to run Windows (x86_64) applications with Wine and Box86/Box64.<br>
This repository stores the latest updates for the Winlator app source.<br>
For more information and releases, please visit the main repository: https://github.com/brunodev85/winlator

# Credits and Third-party apps

- GLIBC Patches by [Termux Pacman](https://github.com/termux-pacman/glibc-packages)
- Wine ([winehq.org](https://www.winehq.org/))
- Box86/Box64 by [ptitseb](https://github.com/ptitSeb)
- Mesa (Turnip/Zink/VirGL) ([mesa3d.org](https://www.mesa3d.org))
- DXVK ([github.com/doitsujin/dxvk](https://github.com/doitsujin/dxvk))
- VKD3D ([gitlab.winehq.org/wine/vkd3d](https://gitlab.winehq.org/wine/vkd3d))
- CNC DDraw ([github.com/FunkyFr3sh/cnc-ddraw](https://github.com/FunkyFr3sh/cnc-ddraw))

Special thanks to all the developers involved in these projects.<br>
Thank you to all the people who believe in this project.