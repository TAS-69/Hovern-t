<p align="center"><img src="hovernt_logo.png" width="256" alt="Hovern't"></p>

# Hovern't

Read enchantment names directly in the villager trade screen. No hovering required. Your cursor has other places to be.

![Expanded merchant screen](docs/screenshots/expanded.png)

Hovern't displays each book's translated enchantment names and levels beside its trade. It reads the enchantments on the actual item, so registered modded enchantments work automatically without a whitelist or per-mod setup.

- Multiple enchantments appear together, with scrolling text for long names.
- Curses and custom enchantment colors keep their formatting.
- **>** collapses the sidebar; **<** expands it to the left.
- The connected interface stays centered and keeps your trade-list scroll position.
- Native merchant textures, buttons, and fonts follow your resource pack.

![Collapsed merchant screen](docs/screenshots/collapsed.png)

| Minecraft | Fabric | NeoForge |
| --- | --- | --- |
| **1.20.1** | Supported | Supported, legacy 47.1.106 |
| **26.3** | Experimental | Experimental |

1.20.1 is the primary target. Intermediate versions are not available yet. The 26.3 builds compile and pass enchantment data checks; their in-game validation is still pending.

Install the JAR matching your Minecraft version and loader on the **client**. The server does not need Hovern't. The Fabric build includes its resource-loading dependencies.

The [wiki](https://github.com/TAS-69/Hovern-t/wiki) contains [installation and troubleshooting](https://github.com/TAS-69/Hovern-t/wiki/Installation), [resource-pack support](https://github.com/TAS-69/Hovern-t/wiki/Resource-Packs), [compatibility verification](https://github.com/TAS-69/Hovern-t/wiki/Compatibility-Verification), and [building and testing](https://github.com/TAS-69/Hovern-t/wiki/Development).

[MIT licensed](LICENSE). Embedded Fabric modules retain their own licenses.
