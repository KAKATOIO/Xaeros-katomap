# Xaero's Katomap

Maquete 3D do terreno que você já explorou, para **Minecraft Java 26.3 (Fabric)**.
Gire a "maquete" para qualquer lado, incline, mova e dê zoom in/out, com bússola, marcador do jogador e coordenadas do bloco sob o cursor.

> Addon **independente**: funciona ao lado do Xaero's World Map / Minimap, mas **não modifica nem depende do código deles**.
> Não é afiliado nem endossado pela Xaero.

## Recursos
- Visão 3D orbital com relevo, sombreamento por inclinação, névoa de distância e água com profundidade.
- Zoom (scroll), girar/inclinar (botão esquerdo), mover (botão direito/meio ou WASD), exagero de altura.
- Bússola (N/S/L/O) que acompanha a rotação e marcador vermelho na posição do jogador.
- Coordenadas **X Y Z** do bloco sob o cursor.
- Qualidade adaptativa: baixa resolução enquanto você mexe, alta quando para (sem travar).
- Memória por dimensão (Overworld, Nether, End), limpa ao sair do mundo/servidor.
- Textos em português (pt-BR) e inglês.

## Instalação (jogadores)
1. Instale o [Fabric Loader](https://fabricmc.net/use/) para o Minecraft **26.3**.
2. Na pasta `mods` (Windows: `%appdata%\.minecraft\mods`) coloque:
   - `xaeros-katomap-<versão>.jar` (esta mod)
   - **Fabric API**
   - *(opcional)* Xaero's World Map e Xaero's Minimap
3. Entre em um mundo e aperte **K**. A tecla pode ser trocada em *Controles > Xaero's Katomap*.

## Controles
| Ação | Como |
|---|---|
| Girar / inclinar | Botão esquerdo + arrastar |
| Mover o mapa | Botão direito ou meio + arrastar, ou **W A S D** |
| Zoom | Scroll do mouse |
| Girar pelo teclado | **Q** / **E** |
| Centralizar no jogador | **R** |
| Exagero de altura | **PgUp** / **PgDn** |
| Qualidade (1 baixa, 2 média, 3 alta) | **1** / **2** / **3** |
| Fechar | **Esc** |

## Como o mapa é montado
O mod lê os chunks que o jogo já carregou (altura do bloco mais alto + cor de mapa do bloco) e guarda isso em memória enquanto você joga.
Por isso, **só aparece o que passou perto de você nesta sessão** (e dentro da distância de renderização).
Áreas nunca vistas aparecem como "base" escura. Explorar e reabrir o mapa preenche a maquete.

## Compilar
### Pelo GitHub (sem instalar nada)
1. Faça commit/push na branch `main`. A aba **Actions** compila sozinha (2 a 5 min).
2. Abra a execução concluída e baixe o artefato **xaeros-katomap-jar**.
3. Para publicar uma versão baixável: crie uma tag `v1.0.0` (ex.: `git tag v1.0.0 && git push origin v1.0.0`).
   O jar aparece em **Releases**.

### No seu PC
Requisitos: **JDK 25** e **Gradle** (ou gere o wrapper com `gradle wrapper` uma vez).
```
gradle build
```
O jar fica em `build/libs/xaeros-katomap-<versão>.jar` (não use o `-sources.jar`).

As versões de Loader / Fabric API / Loom ficam em `gradle.properties`.

## Problemas comuns
- **"Plugin ... not found"/erro do Loom:** confira o id e a versão do plugin em https://fabricmc.net/develop e ajuste `build.gradle`/`gradle.properties`.
- **Erro de compilação com nome de classe/método:** a API do Minecraft 26.x ainda muda entre versões. Abra uma issue ou envie o log do Actions.
- **Mapa vazio:** ande um pouco para carregar chunks e reabra (o aviso "Ainda sem dados" aparece).
- **Lento em PC fraco:** aperte **1** (qualidade baixa) e diminua o zoom.

## Limitações e ideias futuras
- Renderização por software (CPU); uma versão com malha 3D na GPU seria mais rápida e bonita.
- Não lê os dados salvos do Xaero's World Map (só os chunks carregados na sessão). Integração via Mixin e/ou cache em disco por mundo são os próximos passos.
- Sem waypoints do Xaero na maquete.

## English summary
Client-side Fabric mod for Minecraft Java 26.3 that shows an orbitable 3D "diorama" of the terrain you have explored (press **K**).
Drag to rotate, right-drag to pan, scroll to zoom. Independent from Xaero's mods. Build with JDK 25 + Gradle, or use the included GitHub Actions workflow.

## Licença
MIT, veja [LICENSE](LICENSE).
