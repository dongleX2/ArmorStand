# 盔甲架

盔甲架是一款 Minecraft 1.21.8 的玩家模型 mod，支持加载外部模型以替换原版的玩家模型。

[![MC 百科](https://img.shields.io/badge/MC_%E7%99%BE%E7%A7%91-blue?style=for-the-badge)](https://www.mcmod.cn/class/20046.html)
[![Modrinth](https://img.shields.io/modrinth/dt/armor-stand?style=for-the-badge&logo=modrinth)](https://modrinth.com/mod/armor-stand)

## 功能

目前 mod 在早期开发中，支持下列功能：

- 渲染 glTF、VRM、PMX、PMD 格式的模型
- 导入 VMD 格式的动画文件
- 支持实例化渲染，从而提升多个模型渲染时的性能
- 支持多人游戏（可以同步显示模型，但是不能也不会支持同步模型文件）

如果发现了任何问题，或者有任何想法，欢迎在 [issue 区](https://github.com/fifth-light/ArmorStand/issues) 和 [discussion 区](https://github.com/fifth-light/ArmorStand/discussions) 提出。

## 使用方式

首先，你需要自己准备模型，在使用模型前请先确认模型的版权。准备好模型文件后，将模型放在游戏目录下的 `models` 目录以加载。

在游戏内按下 `I` 键可以打开 mod 设置，在设置界面内可以选择你准备好的模型。

在游戏内按下 `K` 键可以打开动画控制界面，在其中你可以加载动画文件（也需要放在 `model` 目录下），并且可以控制动画速度和播放进度。

## 多人游戏

mod 支持多人游戏同步显示模型。在使用前请确保你的服务端也安装了本 mod（如果服务器没有安装，其他玩家将无法看到你的模型，但你自己的模型可以正常显示）。

考虑到分发模型的版权问题，本 mod 不能也不会支持同步模型文件。你需要确保其他玩家也将你使用的模型放置在 `models` 目录下，别的玩家才能显示你的模型。

mod 采取同步模型哈希值的方式，因此你可以重命名模型，甚至可以放在子目录下，只要模型内容不变，你就可以看到其他玩家的模型。 

## 鸣谢

感谢 [Saba](https://github.com/benikabocha/saba) 项目为 MMD（PMX/PMD/VMD 格式）的逻辑提供了参考。

## 许可证

本 mod 以 LGPL 3.0 及以上版本授权，在发布和修改时请遵守许可证要求。

虽然不是强制要求，如果你的作品（例如视频等）使用到了本 mod，请声明使用了本 mod，如果可以的话，还可以带上本 mod 的链接。

---

# ArmorStand

ArmorStand is a Minecraft 1.21.8 player model mod that supports loading external models to replace the vanilla player
model.

Currently, the mod is in early development, and supports the following features:

- Render glTF, VRM, PMX, PMD models
- Import VMD format animation files
- Support instance rendering, improving performance when rendering multiple models
- Support multiplayer (can synchronize model display, but won't support synchronizing model files)

If you encounter any problems or have any ideas, please feel free to open an issue or discussion in the [issue area](https://github.com/fifth-light/ArmorStand/issues) and [discussion area](https://github.com/fifth-light/ArmorStand/discussions).

## Usage

First, you need to prepare a model. Before using any model, please check the copyright of the model to avoid copyright issues. Once you have prepared the model file, place it in the `models` directory to load it.

In game, press `I` to open the mod settings, and choose the model you prepared.

In game, press `K` to open the animation control interface, where you can load animation files (you also need to place them in the `models` directory), and control the animation speed and playback progress.

## Multiplayer

The mod supports multiplayer model synchronization. Before using it, please ensure that your server also has the mod installed (if the server does not have the mod, other players will not be able to see your model, but your own model can still be displayed).

Considering distributing models causes copyright issues, the mod can not and will not support synchronizing model files. You need to ensure that other players will also place your model in the `models` directory, so that they can see your model.

The mod uses the model's hash value to synchronize models, so you can rename the model, even put it in a subdirectory. As long as the model content does not change, you can see other players' models.

## Acknowledgments

Thanks for [Saba](https://github.com/benikabocha/saba) project for providing reference for MMD (PMX/PMD/VMD format).

## License

The mod is licensed under the LGPL 3.0 or later. Please comply with the license requirements when distributing and modifying.

Although it is not required, if your work (such as videos, etc.) uses the mod, please declare that you are using the mod, and if possible, also include a link to the mod.
