# QuickLauncher

QuickLauncher é um launcher Android nativo, minimalista e pensado para dispositivos
usados horizontalmente. O MVP mostra uma faixa de atalhos em tela cheia e abre o
aplicativo correspondente ao toque.

## Tecnologias

- Kotlin
- Jetpack Compose
- Gradle Kotlin DSL
- Android SDK 35 (`minSdk 28`)
- AndroidX `WindowCompat` e `WindowInsetsControllerCompat`

O projeto tem apenas o módulo `app`, não usa banco de dados, injeção de dependência
ou permissões especiais.

## Requisitos

- Android Studio com JDK 17 configurado como Gradle JDK
- Android SDK Platform 35 e Build Tools 37.0.0
- Dispositivo Android 9 ou superior
- ADB para instalação e diagnóstico em dispositivo físico

No Arch Linux desta máquina, o JDK recomendado está em
`/usr/lib/jvm/java-17-openjdk` e o SDK em `/opt/android-sdk`. `local.properties`
é uma configuração local e não deve ser versionado.

## Estrutura

```text
app/src/main/java/com/valmo/quicklauncher/
├── MainActivity.kt
├── data/
│   └── DefaultShortcuts.kt
├── launcher/
│   ├── AppLauncher.kt
│   ├── LauncherScreen.kt
│   └── LauncherViewModel.kt
├── model/
│   └── AppShortcut.kt
└── ui/theme/
    ├── Color.kt
    ├── Theme.kt
    └── Type.kt
```

- `DefaultShortcuts` é o único lugar que contém a configuração inicial dos apps.
- `AppLauncher` conversa com o Android para localizar ícones e abrir atividades.
- `LauncherViewModel` prepara o estado exibido pela tela.
- `LauncherScreen` contém somente a interface e funciona com dados falsos no Preview.

## Como um launcher Android funciona

Uma atividade que declara as categorias `HOME` e `DEFAULT` para a ação `MAIN`
pode ser escolhida pelo Android como tela inicial. Depois da instalação, o sistema
pode mostrar um seletor ao pressionar Home. Selecionar QuickLauncher como padrão
faz com que essa atividade seja aberta no lugar do launcher anterior.

O manifesto declara visibilidade somente para os seis pacotes consultados pelo MVP.
Não é utilizada a permissão ampla `QUERY_ALL_PACKAGES`.

## Compilar e testar

Confirme primeiro o JDK:

```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk
./gradlew assembleDebug
./gradlew test
./gradlew lint
```

O APK debug é gerado em:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Instalar e abrir

```bash
adb devices
./gradlew installDebug
adb shell am start -n com.valmo.quicklauncher/.MainActivity
```

Para simular o botão Home:

```bash
adb shell input keyevent KEYCODE_HOME
```

## Selecionar o launcher padrão

1. Instale e abra o QuickLauncher.
2. Pressione o botão Home.
3. Escolha **QuickLauncher**.
4. Se desejar, escolha **Sempre**.

Também é possível abrir a tela de configuração de aplicativo residencial:

```bash
adb shell am start -a android.settings.HOME_SETTINGS
```

Os nomes dos menus podem variar entre fabricantes. Em geral, a opção fica em
**Configurações > Apps > Apps padrão > App de início**.

## Retornar ao launcher anterior

Abra a configuração de aplicativo residencial e selecione novamente o launcher
do fabricante:

```bash
adb shell am start -a android.settings.HOME_SETTINGS
```

Se o QuickLauncher estiver aberto, o atalho **CONFIGURAÇÕES** também dá acesso às
configurações do Android. Desinstalar o QuickLauncher remove-o das opções de Home:

```bash
adb uninstall com.valmo.quicklauncher
```

## Descobrir package names

Liste todos os pacotes ou filtre um nome conhecido:

```bash
adb shell pm list packages
adb shell pm list packages | grep -i atak
adb shell pm list packages | grep -Ei 'camera|clock|chrome|maps'
```

Verifique se um pacote específico está instalado:

```bash
adb shell pm path com.atakmap.app.civ
```

Os pacotes de câmera, relógio e navegador podem mudar conforme o fabricante.
Este MVP está configurado para a câmera Motorola (`com.motorola.camera3`).
Altere somente a lista em `DefaultShortcuts.kt` e a entrada correspondente em
`<queries>` quando precisar adaptar o QuickLauncher a outro dispositivo.

## Logs

Todos os erros úteis de resolução, ícone e abertura usam a tag `QuickLauncher`:

```bash
adb logcat
adb logcat -s QuickLauncher
```

Para diagnosticar filtros de visibilidade durante o desenvolvimento:

```bash
adb shell pm log-visibility --enable com.valmo.quicklauncher
```

Desative depois do teste, pois esse log adicional tem custo:

```bash
adb shell pm log-visibility --disable com.valmo.quicklauncher
```

## Esboço da tela

```text
┌─────────────────────────────────────────────────────────────────────┐
│                                                                     │
│     ATAK       │    CÂMERA     │    MAPAS      │   RELÓGIO          │
│                │               │               │                    │
│     [ícone]    │    [ícone]    │    [ícone]    │   [ícone]          │
│                │               │               │                    │
└─────────────────────────────────────────────────────────────────────┘
```

## Fluxo principal

```text
Android inicia ou usuário pressiona Home
                ↓
QuickLauncher é apresentado
                ↓
Usuário toca em um atalho
                ↓
Launcher resolve o Intent
       ┌────────┴────────┐
       ↓                 ↓
App encontrado      App indisponível
       ↓                 ↓
Abre o app         Exibe mensagem
```

## Limitações atuais

- Os atalhos são configurados no código.
- Pacotes alternativos de fabricantes não são descobertos automaticamente.
- Não há persistência, reordenação ou edição na interface.
- O modo imersivo não impede o usuário ou o sistema de revelar as barras.
- Não há kiosk mode, device owner, widgets ou notificações.

## Roadmap

1. Tela de edição de atalhos.
2. Seleção de aplicativos instalados.
3. Persistência com DataStore.
4. Quantidade configurável de colunas.
5. Reordenação dos atalhos.
6. Toque longo para configuração.
7. Botão protegido para sair ou abrir configurações.
8. Modo kiosk/device owner opcional.
9. Inicialização e recuperação após reinício.
10. Perfis diferentes de atalhos.
