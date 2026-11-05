# ⚽ NEWGEN LAUNCHER

> **Universal WebGL 3D Game Launcher for Android**

Esse APK funciona como um emulador/launcher dedicado: você instala o aplicativo **uma única vez** no celular. Sempre que você atualizar o jogo (Three.js/React/HTML5), basta copiar o build para a pasta do celular:

```
📁 /sdcard/NEWGEN/game/
   ├── index.html
   └── assets/
```
*(Ou compactar tudo em `/sdcard/NEWGEN/game.zip` que o Launcher descompacta automaticamente na inicialização).*

---

## ⚡ Recursos do Launcher

- **Modo Tela Cheia Imersivo (Landscape):** Esconde barra de status e barra de navegação virtual automaticamente.
- **Aceleração 3D por Hardware (WebGL 2.0):** Roda jogos Three.js pesados a 60 FPS com shaders e sombras.
- **Permissão de Armazenamento Automática (`MANAGE_EXTERNAL_STORAGE`):** Lê os arquivos locais do armazenamento interno sem bloqueios do Android 11+.
- **Auto-Unzip:** Se você soltar um arquivo `game.zip` em `/sdcard/NEWGEN/`, o launcher descompacta e inicia o jogo sozinho!
- **Tela de Fallback Elegante:** Se a pasta estiver vazia, exibe instruções claras de onde colocar os arquivos.

---

## 🚀 Como Copilar o APK

O repositório está configurado com **GitHub Actions** para compilar automaticamente a cada push na branch `main`:
1. Dê push para a branch `main`.
2. Acompanhe a aba **Actions**.
3. Baixe o artefato `NEWGEN-LAUNCHER-APK` ou a Release gerada.
