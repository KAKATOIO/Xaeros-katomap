# Xaero kaka (Minecraft 26.3, Fabric)

Maquete 3D do terreno já explorado, no estilo "mapa de mesa". Funciona ao lado do Xaero's World Map / Minimap,
mas NÃO modifica nem depende dos jars deles (a licença deles proíbe redistribuição/modificação).

## Compilar
1. Instale o JDK 25.
2. Abra https://fabricmc.net/develop , escolha Minecraft 26.3 e copie para `gradle.properties`
   `loader_version`, `fabric_api_version` e `loom_version` (e o id do plugin em `build.gradle`, se for diferente).
3. Rode `gradle wrapper` (uma vez) e depois `./gradlew build`.
4. O jar fica em `build/libs/xaero-kaka-1.0.0.jar`. Coloque na pasta `mods` junto com o Fabric API.

## Uso
Tecla **K** (configurável em Controles > Xaero kaka) abre a maquete.
- Botão esquerdo + arrastar: girar/inclinar | Direito ou meio + arrastar: mover
- Scroll: zoom | W/A/S/D: mover | Q/E: girar | R: centralizar no jogador
- PgUp/PgDn: exagero de altura | 1/2/3: qualidade | Esc: sair
O mapa usa os chunks que o jogo já carregou (cache guardado na sessão, por dimensão).
