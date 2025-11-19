<div align="center">
  <img src="https://i.imgur.com/wkSVGXV.png" style="width: 20%;" alt="Icon">

  <h1>fembyte</h1>
  <p>
    A personal Paper fork focused on stable TPS and a comfy experience.  
  </p>
</div>

---

## What is this?

**fembyte** is my personal fork of [PaperMC](https://papermc.io/) that I am building alongside my YouTube channel + Discord community.

The goal isn't to compete with big server cores, it's to explore:
- performance improvements
- code that's readable
- a server that feels smooth

---

## Features (so far)

- **Optimized random ticking**  
  Smarter random tick system to increase performance, while still feeling natural.

- **Dynamic tick throttling when TPS drops**  
  When the server starts to fall behind, certain systems are throttled to prevent TPS from dying.

- **Async mob spawning (safe)**  
  Heavy calculations for mob spawning run asynchronously.

---

## Building

```bash
# clone the repo
git clone https://github.com/dractical/fembyte.git
cd fembyte

# patches/build
./gradlew applyAllPatches
./gradlew createMojmapPaperclipJar
