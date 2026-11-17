# Журнал изменений AetherClient (Staging Workspace)

## [Обновление 2.5.0-RELEASE] — Финальная сборка: 16 исправлений, вкладка «Инфо», менеджер конфигов, AspectRatio под худом

Сборка и тесты проверены:
`./gradlew :bridge:build :client:remapJar :agent:build :launcher:desktopJar :agent:test --console=plain` → **BUILD SUCCESSFUL** (Все тесты пройдены).

### Ключевые нововведения и исправления:
1. **Устранение рывков и отставания ESP/моделей при спринте**:
   - В `WorldToScreen` исключены скачки динамического FOV спринта ванильного Minecraft через `GameRenderer.getFov(...)`.
   - Проекция сущностей и моделей теперь абсолютно монолитна на любой скорости перемещения.
2. **Свободная камера (Freecam)**:
   - Камера полностью откреплена от тела игрока: физическое тело стоит на месте, летит только виртуальная камера без пакетов полета на сервер.
3. **Объемные 3D-частицы в игровом мире**:
   - Вместо плоских точек на экране частицы теперь имеют честную мировую координату `(X, Y, Z)`, крутятся в 3D и проецируются через матрицу камеры.
4. **Скрытие ванильного прицела (Crosshair)**:
   - При активном клиентском прицеле ванильный прицел перехватывается и скрывается в `AetherClassTransformer` (`InGameHud.renderCrosshair`).
5. **Честный Motion Blur**:
   - `KawaseBlur` переведен на захват активного Minecraft FBO (`GL30.GL_FRAMEBUFFER_BINDING`), размытие работает без артефактов и черных экранов.
6. **Отображение HUD в чате и меню**:
   - В `ChatScreen` HUD остается видимым и виджеты можно свободно перетаскивать кликом мыши. В `GameMenuScreen` HUD отображается статично.
7. **Траектории полета снарядов (Projectiles)**:
   - Траектория рисуется только когда предмет зажат в руке; блок-рейкаст находит точное место приземления с маркером падения; также отслеживаются летящие стрелы и жемчуга.
8. **Плавный Fullbright (Hysteresis & Anti-flicker)**:
   - Введена задержка 15 тиков на переключение, исключающая мигание гаммы на границах света.
9. **4 стиля карточки Target HUD**:
   - «Стекло» (Liquid Glass с матовым размытием)
   - «Целестиал» (неоновые сияющие обводки, градиент)
   - «Компакт» (минималистичный киберспортивный вид)
   - «Киберспорт» (технологичная карточка с полосой брони и HP)
10. **Круги приземления JumpCircles «Целестиал»**:
    - Концентрические расширяющиеся кольца с вращающимися неоновыми рунами и надписью «AETHER» по дуге.
11. **Маскировка ников NameProtect**:
    - Маскирует имя игрока в табе, чате, скорборде и над головой на настраиваемый псевдоним.
12. **HitColor через NativeImage OverlayTexture**:
    - Модели окрашиваются в 3D непосредственно через текстуру урона Minecraft, без наложения плоских 2D-коробок.
13. **Строгая проверка прямой видимости (Wallhack Protection)**:
    - Рейкаст строго требует дохождения луча до хитбокса цели (глаза, центр, ноги), сквозь стены ничего не видно при `throughWalls = false`.
14. **AspectRatio с опцией «ХУД поверх полос»**:
    - Добавлена настройка `hud_on_top`: кинематографичные полосы могут рендериться как под элементами HUD, так и поверх всего экрана.
15. **Вкладка «Инфо» в ClickGUI**:
    - Версия клиента: `v1.2.0-RELEASE`
    - Статус: `STABLE RELEASE (Material 3 + Liquid Glass)`
    - Время сессии в игре (Playtime HH:mm:ss)
    - Профиль игрока, FPS
    - Официальные контакты с кнопкой копирования в буфер обмена:
      - Discord: `nelttan.gg`
      - Telegram: `@shadowwtls`
      - Telegram: `@Phant0mNight`
16. **Вкладка «Конфиги» в ClickGUI**:
    - Полноценный in-game менеджер конфигураций: список сохраненных конфигов, создание нового по имени, кнопки «Загрузить», «Записать», «Удалить» и «Сбросить всё к дефолтным» с мгновенными toast-уведомлениями.

---

## [Обновление 2.4.0] — Комплексный апгрейд визуалов, анимаций, 3D-анкоров, анти-читерского анхука и Material 3 / Liquid Glass

Все модификации синхронизированы в main-репозиторий и staging-репозиторий. Проверены сборка и тесты:
`./gradlew :bridge:build :client:compileJava :client:remapJar :agent:build :launcher:desktopJar :launcher:desktopTest :agent:test --console=plain` → **BUILD SUCCESSFUL** (23/23 тестов пройдены).

### Ключевые изменения:

1. **Абсолютный Unhook & Восстановление байткода на 100% (HotSpot Retransformation)**:
   - В `AetherAgentMain.java` реализована процедура полного анхука:
     - Снятие `activeTransformer` из JVM.
     - Вызов `inst.retransformClasses(...)` для всех перехваченных классов (`MinecraftClient`, `GameRenderer`, `Mouse`, `Keyboard`, `Window`), восстанавливающий исходный чистый ванильный байткод без следов вызовов бриджа.
     - Очистка системных свойств (`aether.agent.loaded`, `aether.client.active`).
     - Рекурсивное стирание временного распакованного каталога JAR/библиотек.
     - Закрытие и сборка изоляционных `URLClassLoader`.
     - Двухфазный вызов `System.gc()`.
   - В `AetherClientMain.java` и `ClickGuiModule.java` добавлена горячая клавиша паники (`panic_key`, либо стандартное сочетание `Ctrl + Shift + Delete`) для моментального полного анхука при проверке.

2. **Защита от Wallhack (честные визуалы по умолчанию)**:
   - В модулях **ESP**, **Chams**, **Tracers**, **Arrows**, **Item ESP**, **Hole ESP** добавлена настройка `through_walls` (по умолчанию `false`!).
   - При `through_walls = false` все визуалы строго учитывают поле зрения и преграды (`canSee` / `RaycastContext`). Модули больше не светят сквозь стены, если игрок этого явно не включит.
   - Добавлены новые стили для ESP: «Точки», «Скобка», «Стекло», «Рамка», «Углы».

3. **Косметика (Cosmetics) и поддержка вида от 1-го лица**:
   - В `CosmeticsModule.java` и `VisualRenderer.java` добавлен флаг `first_person` («От первого лица») и смещение `fp_offset_y`. В режиме от 1-го лица косметика плавно проецируется чуть впереди взгляда игрока.
   - Добавлено смещение высоты `height_offset` для точной подгонки под любой скин.
   - Добавлены 7 разнообразных стилей косметики:
     - **Шляпа** (конус с полями в стиле китайской шляпы/мага)
     - **Крылья** (с реалистичной физикой взмахов `flap_speed`, 3 формы: Ангельские, Демонические, Фея)
     - **Нимб** (тороидальное кольцо с наклоном, пульсацией и отражениями)
     - **Корона** (королевская корона с 5 зубцами, драгоценными сферами и сиянием)
     - **Орбитальные сферы** (орбитальные шары вокруг головы с настраиваемым количеством от 1 до 8)
     - **Аура пламени** (вихрь огненных искр, поднимающихся по спирали)
     - **Кошачьи ушки** (милые неоновые ушки с градиентом внутренней части)

4. **Target HUD с динамическим следованием за ногами/головой в 3D**:
   - В `TargetHudModule.java` и `TargetHudElement.java` добавлен выбор позиции: `Экран (HUD)`, `Под ногами`, `Над головой`, `Сбоку`.
   - Карточка врага в реальном времени проецируется через `WorldToScreen` прямо под ноги или над головой противника с плавным сглаживанием EMA (`smooth_follow`), динамическим масштабированием от расстояния, золотой полоской поглощения (Absorption) и чипами прочности брони противника.

5. **100% надёжный Hit Color**:
   - Устранена проблема с несрабатыванием вспышек урона: в `VisualRenderer.java` реализовано считывание `living.hurtTime > 0` непосредственно у живых сущностей.
   - Цвет урона гарантированно срабатывает на ЛЮБЫЕ источники повреждений: удары мечом, выстрелы из лука, урон от мобов, падение, зелья.
   - Поддерживаются стили: «Стекло», «Неоновый ореол», «Вспышка», «Силуэт», «Коробка», а также фильтр целей: «Все», «Только игроки», «Только цель», «Только мобы».

6. **Автоматическое включение/выключение Fullbright**:
   - В `FullbrightModule.java` добавлен режим `auto_toggle` с условиями: «В темноте» (порог света `light_threshold`), «Ночью» (`!world.isDay()`), «Под землей» (`Y < 58` или темный блок).

7. **Новые модули из референсов Kimiko / топовых клиентов**:
   - **HpCounterModule** (`hp_counter`): всплывающие 3D-цифры урона (`-X.X`) и лечения (`+X.X`) над головами сущностей с физикой подъема, затуханием, pill-фоном Liquid Glass и уважением преград.
   - **SwingAnimationModule** (`swing_anim`): стили взмаха оружия («1.7 Vanilla», «Плавный», «Размашистый», «Вращение», «Компактный»), регулировка скорости и наклона.
   - **HitParticlesModule** & **JumpCirclesModule** & **TrailsModule**: глубоко расширены новыми стилями, физикой, волной при приземлении (`landing`), гравитацией и свечением.

---

## 1. Добавленные модули и HUD-элементы

### Новые модули (19 шт., `client/.../module/impl/`)

1. **ChamsModule** (`chams`, категория VISUAL)
   Силуэтная подсветка сущностей сквозь стены. Настройки: форма (`Коробка/Углы/Свечение/Скобка`), палитра цвета (`Статика/Радуга/Тема/Градиент`), цвет, скорость цвета, плотность заливки, толщина линии, дистанция, флаги `Игроки/Враждебные/Мирные`.

2. **MotionBlurModule** (`motion_blur`, VISUAL)
   Кинематографичный направленный блюр при резком повороте камеры. Настройки: сила, чувствительность. Реализован как полноэкранный линейный градиент от края экрана по направлению движения (без пост-обработки).

3. **FreecamModule** (`freecam`, VISUAL)
   Свободный полёт камеры сквозь блоки (noclip + fly) с сохранением/восстановлением `PlayerAbilities` и `noClip`. Настройки: скорость полёта, проходить сквозь блоки.

4. **BreadcrumbsModule** (`breadcrumbs`, VISUAL)
   Неоновая 3D-лента/точки следов движения с плавным затуханием. Настройки: стиль (`Лента/Точки`), палитра, цвет, скорость цвета, толщина, количество точек, время затухания.

5. **ProjectilesModule** (`projectiles`, VISUAL)
   Расчёт и отрисовка баллистической траектории снарядов. Настройки: тип (`Все/Лук/Жемчуг/Снежки`), палитра, цвет, скорость цвета, число точек, толщина, масштаб гравитации.

6. **ItemEspModule** (`item_esp`, VISUAL)
   Подсветка выпавших предметов с названием, количеством и свечением. Настройки: форма, палитра, цвет, скорость цвета, толщина, дистанция, `Название/Количество/Свечение`.

7. **BlockOverlayModule** (`block_overlay`, VISUAL)
   Рамка целевого блока + кольцо прогресса ломания (прогресс читается рефлексией из `ClientPlayerInteractionManager`). Настройки: форма, палитра, цвет, скорость цвета, толщина, `Прогресс ломания`, радиус кольца.

8. **HoleEspModule** (`hole_esp`, VISUAL)
   Подсветка безопасных 2x2-ям (бедрок/обсидиан, либо любой полный блок). Настройки: форма, палитра, цвет, скорость цвета, толщина, радиус поиска, `Только бедрок`.

9. **HitColorModule** (`hit_color`, COMBAT)
   Кастомная вспышка-подсветка сущности при попадании. Настройки: цвет, длительность, яркость.

10. **WeatherModule** (`weather`, VISUAL)
    Клиентский контроль погоды (`Чисто/Дождь/Гроза/Снег`). Настройки: режим, сила дождя, сила грозы. Состояние мира сохраняется/восстанавливается при отключении.

11. **TimeChangerModule** (`time_changer`, VISUAL)
    Произвольное время суток + скорость течения времени + тонировка неба (градиент верх/низ). Настройки: своё время, время суток (0–24000), скорость, тонировка, верхний/нижний цвет, плотность тона.

12. **NoRenderModule** (`no_render`, VISUAL)
    Скрывает оверлеи: огонь, тряску урона (hurt cam через `damageTiltStrength`), слепоту/тьму/тошноту. Настройки: 5 булевых флагов.

13. **CustomScoreboardModule** (`scoreboard`, HUD)
    Стилизованный скорборд в духе Material 3. Данные читаются рефлексией (`getObjectiveForSlot` / `getAllPlayerScores`) с корректной деградацией. Настройки: сторона, палитра, цвет, скорость цвета, плотность стекла, `Заголовок`, `Красные очки`.

14. **CustomTabListModule** (`tablist`, HUD)
    Стилизованный список игроков. Настройки: палитра, цвет, скорость цвета, плотность стекла, число колонок, `Пинг`, `Заголовок`.

15. **CosmeticsModule** (`cosmetics`, VISUAL)
    Нимб / китайская шляпа / крылья вокруг персонажа (проецируются 3D→2D). Настройки: тип, палитра, цвет, скорость цвета, размер, скорость вращения, прозрачность.

16. **NotificationsModule** (`notifications`, CLIENT, включён по умолчанию)
    Всплывающие стеклянные тосты о переключении модулей и событиях. Настройки: позиция, длительность, макс. тостов, плотность стекла, палитра, цвет акцента, скорость цвета.

17. **AutoSprintModule** (`auto_sprint`, COMBAT)
    Авто-бег вперёд. Настройки: не сбивать бег.

18. **ColorFilterModule** (`color_filter`, VISUAL)
    Полноэкранная цветокоррекция (`Тёплый/Холодный/Сепия/Моно`). Настройки: фильтр, сила.

19. **ItemPhysicsModule** (`item_physics`, VISUAL)
    Непрерывное вращение выпавших предметов вокруг вертикальной оси (Y). Настройки: скорость вращения (об/с). Реализовано через ASM-инъекцию в `ItemEntityRenderer.method_3996` (`class_916`): при включённом модуле пре-мультиплицируется матрица модели `RotationAxis.POSITIVE_Y.rotation(angle)`. Угол вычисляется как wall-clock time × скорость; при выключении — 0 (инъекция коротко замыкается). В `AetherBridge` добавлены `setItemSpinAngle/getItemSpinAngle`. Юнит-тест `transformsItemEntityRendererSpinInjection` верифицирует наличие инъекции и идемпотентность.

### Новые HUD-виджеты (5 шт., `client/.../hud/impl/`)

1. **CompassHud** — вращающийся компас с кардинальными направлениями, центральной иглой и подписью биома/измерения (биом читается рефлексией, фолбэк на измерение).
2. **SpeedometerHud** — скорость в блоках/сек + мини-график.
3. **FpsPingGraphHud** — график FPS и пинга в реальном времени.
4. **InventoryHud** — компактное превью хотбара (9 слотов) с выделением активного слота, количеством и полосой прочности.
5. **ArmorHudPro** — прочность брони в процентах + оставшееся число ударов + полоса прочности.

Каждый виджет управляется своим тумблером и масштабом в `HudModule`.

---

## 2. Новые настройки в существующих модулях

- **HudModule** — добавлены тумблеры `compass`, `speedometer`, `fps_ping`, `inventory`, `armor_pro` и масштабы `scale_compass`, `scale_speed`, `scale_fps_ping`, `scale_inventory`, `scale_armor_pro`.
- **Setting** (базовый класс) — добавлено поле `defaultValue` и метод `resetToDefault()`.
- **BooleanSetting** — переопределён `resetToDefault()` с синхронизацией `animProgress`.
- **Module** — добавлено поле `pressSpring` (пружина для клика) и метод `resetSettings()`.

---

## 3. Архитектурные изменения и рендеринг

- **AetherPalette** — добавлены `resolvePalette(mode, base, time, speed)` (Статика/Радуга/Тема/Градиент) и статический `rainbow(float)`.
- **Render2D** — добавлены примитивы `drawBracketRect` (скобка), `drawGlowBox` (свечение), `drawArc` (частичное кольцо), `drawScreenEdgeGradient` (краевой градиент для motion blur); добавлен `default` в switch `drawIcon` (фикс checkstyle).
- **VisualRenderer** — крупный рефакторинг:
  - Объектный пулинг (`Pool<T>`) для `JumpCircle`, `TrailPoint`, `BreadcrumbPoint`, `HitSpark`, `AmbientParticle` — убран постоянный `new` в горячих путях.
  - Новые рендер-проходы: `renderChams`, `renderBreadcrumbs`, `renderProjectiles`, `renderItemEsp`, `renderBlockOverlay`, `renderHoleEsp`, `renderHitTints`, `renderCosmetics`, `renderSkyTint`, `renderColorFilter`, `renderMotionBlur`, `renderCustomScoreboard`, `renderCustomTabList`.
  - Отрисовка тостов через `ToastManager.get().render(...)`.
- **ToastManager** (новый, `notify/ToastManager.java`) — стек стеклянных уведомлений, пуш на переключение модулей (`pushModuleToggle`).
- **Spring** (новый, `gui/Spring.java`) — демпфированная пружина для микро-анимаций.
- **ClickGuiController** — контекстное меню модуля (ПКМ): «Настройки / Сменить клавишу / Сбросить / Переключить»; пружинная анимация нажатия бокса и появления меню; расширен `localizeModeValue` (перевод всех новых значений режимов на EN); пуш тостов при переключении.
- **AetherClientMain** — регистрация 5 новых HUD-виджетов; пуш тоста при переключении по кейбинду; расширен `hasActiveOverlayVisuals()` (все новые оверлей-модули + активные тосты).

---

## 4. Новые файлы

Модули (`client/src/main/java/dev/aetherclient/client/module/impl/`):
- `ChamsModule.java`, `MotionBlurModule.java`, `FreecamModule.java`, `BreadcrumbsModule.java`, `ProjectilesModule.java`, `ItemEspModule.java`, `ItemPhysicsModule.java`, `BlockOverlayModule.java`, `HoleEspModule.java`, `HitColorModule.java`, `WeatherModule.java`, `TimeChangerModule.java`, `NoRenderModule.java`, `CustomScoreboardModule.java`, `CustomTabListModule.java`, `CosmeticsModule.java`, `NotificationsModule.java`, `AutoSprintModule.java`, `ColorFilterModule.java`

HUD (`client/src/main/java/dev/aetherclient/client/hud/impl/`):
- `CompassHud.java`, `SpeedometerHud.java`, `FpsPingGraphHud.java`, `InventoryHud.java`, `ArmorHudPro.java`

Вспомогательные:
- `client/src/main/java/dev/aetherclient/client/gui/Spring.java`
- `client/src/main/java/dev/aetherclient/client/notify/ToastManager.java`

Изменённые файлы:
- `client/.../theme/AetherPalette.java`
- `client/.../render/Render2D.java`
- `client/.../render/VisualRenderer.java`
- `client/.../module/setting/Setting.java`, `BooleanSetting.java`
- `client/.../module/Module.java`, `ModuleManager.java`
- `client/.../module/impl/HudModule.java`
- `client/.../gui/ClickGuiController.java`
- `client/.../AetherClientMain.java`
- `bridge/src/main/java/dev/aetherclient/bridge/AetherBridge.java` (флаги подавления скорборда/таба + флаги вращения предметов: `setItemSpinAngle`, `getItemSpinAngle`, `itemSpinAngle`)
- `agent/src/transformer/java/dev/aetherclient/agent/transformer/AetherClassTransformer.java` (цели `class_329`/`class_355`/`class_916` + `SuppressAdapter` + `ItemSpinAdapter`)
- `agent/src/main/java/dev/aetherclient/agent/AetherAgentMain.java` (`HOOK_CLASS_NAMES` + `class_329`/`class_355`/`class_916`)
- `agent/src/test/java/dev/aetherclient/agent/AetherClassTransformerTest.java` (тест подавления скорборда/таба + тест инъекции вращения предметов `transformsItemEntityRendererSpinInjection`)

---

## 5. Заметки для ревьюера (следующей нейросети)

- **Все изменения только в staging.** Основной клиент не трогался.
- **ItemPhysicsModule** теперь реализован (ранее — осознанно не делал). Инъекция в `ItemEntityRenderer.method_3996` (`class_916`) через `ItemSpinAdapter`: если `AetherBridge.getItemSpinAngle() != 0f` — `matrices.multiply(RotationAxis.POSITIVE_Y.rotation(angle))`. Угол — wall-clock time × `spinSpeed` (об/с) × 2π. В тесте проверяется наличие bridge-вызова и matrix multiply в байткоде + идемпотентность трансформации.
- **ToastManager** — исправлена утечка: тосты пушятся только при включённом модуле `NotificationsModule` (в `AetherClientMain.onKey` и `ClickGuiController`). При выключении модуля `onDisable` вызывает `ToastManager.clear()`. Рендер/очистка тостов выполняются только при активном модуле (проверка внутри `VisualRenderer` через `moduleManager.notifications.isEnabled()`).
- **ClickGuiController** — иконка для `ItemPhysicsModule` добавлена (SPARKLES).
- **Подавление ванильных оверлеев реализовано через ASM-трансформер** (не через Fabric Mixin — в этой архитектуре клиент грузится Java-агентом, а не Fabric-лоадером). `AetherClassTransformer` теперь дополнительно хакает `InGameHud` (`class_329`, методы `method_1757`/`method_55803`) и `PlayerListHud` (`class_355`, `method_1919`): в начало методов вставляется «if `AetherBridge.shouldHideScoreboard()/shouldHideTabList()` return». Флаги выставляет `AetherClientMain.onClientTick`. Проверено юнит-тестом `AetherClassTransformerTest.transformsScoreboardAndTabListSuppression` (байткод верифицируется + поведение проверяется). При переносе на другую версию MC — сверь intermediary-имена методов по mappings.
- **Рефлексия как защита от расхождения маппингов.** Используется в `CustomScoreboardModule` (чтение скорборда), `CompassHud` (биом), `NoRenderModule`/`VisualRenderer.readBreakProgress` (прогресс ломания), `WeatherModule.setThundering`, `VisualRenderer.profileName`. Все вызовы обёрнуты в try/catch и деградируют без крашей. Если при переносе маппинги (Yarn → Mojmap) поменяются — правь эти места.
- **`WeatherModule`**: `setRaining` доступен напрямую, а `setThundering` вызывается рефлексией (в текущих Yarn-маппингах сеттер отсутствует/называется иначе).
- **`InventoryHud`**: `PlayerInventory.main`/`selectedSlot` приватны в 1.21.11, поэтому хотбар читается через `getStack(i)`, а активный слот определяется сравнением ссылок с `mc.player.getMainHandStack()`.
- **`ProjectilesModule`** теперь использует точную ванильную баллистику: начальная скорость 3.0 (лук) / 1.5 (жемчуг/снежок), гравитация 0.05 (лук) / 0.03 (жемчуг/снежок), лобовое сопротивление 0.99, интегрирование за тик; `gravityScale` — множитель (1.0 = ваниль).
- **Пулинг в `VisualRenderer`**: при выгрузке (`reset()` / смена мира) объекты возвращаются в пул; `Pool` ограничен 512 элементами.
- **Кейбинды/тосты**: тосты пушятся в двух местах — `AetherClientMain.onKey` и `ClickGuiController` (переключение в GUI). При загрузке конфига и выгрузке тосты **не** пушятся (чтобы не спамить).
- **Сброс настроек** (`Module.resetSettings`) восстанавливает значения по умолчанию, включая `animProgress` у `BooleanSetting`.
- **Сборка проверяется** командой из шапки; при внесении правок повторно прогони `:client:checkstyleMain`, т.к. блокирующие правила — `UnusedImports`, `MissingSwitchDefault`, `EmptyCatchBlock` (имя переменной исключения должно быть `ignored`/`expected`).

---

## 6. Догон (catch-up) — максимальные версии фич

Второй проход по четырём пунктам, которые в первой версии были сделаны «достаточно, но не максимум».

### 6.1 Проектили — точная ванильная физика ✅
`VisualRenderer.renderProjectiles` переписан на точную ванильную баллистику (блоки за тик):
- Лук: `v₀ = 3.0`, гравитация `0.05000000074505806`, drag `0.9900000095367432`.
- Жемчуг / снежок: `v₀ = 1.5`, гравитация `0.03`, drag `0.9900000095367432`.
- Интегрирование за тик: `pos += v; v.y -= g; v *= drag`. `gravityScale` — множитель (1.0 = ваниль).

### 6.2 Скорборд/таб — реальное подавление ванильного слоя ✅
Архитектура здесь не Fabric-Mixin (клиент грузится **Java-агентом** через `AetherClassTransformer` на intermediary-именах), поэтому подавление сделано расширением ASM-трансформера, а не `@Mixin`:
- `bridge/AetherBridge.java`: добавлены флаги + методы `setHideScoreboard/setHideTabList/shouldHideScoreboard/shouldHideTabList`.
- `agent/.../AetherClassTransformer.java`: новые цели `class_329` (InGameHud, `method_1757` + `method_55803`) и `class_355` (PlayerListHud, `method_1919`); добавлен `SuppressAdapter` — «if flag return».
- `agent/.../AetherAgentMain.java`: `class_329`/`class_355` добавлены в `HOOK_CLASS_NAMES` для retransform-пути.
- `client/.../AetherClientMain.java`: `onClientTick` выставляет флаги по `moduleManager.scoreboard/tabList`.
- Юнит-тест `AetherClassTransformerTest.transformsScoreboardAndTabListSuppression` верифицирует байткод и поведение (подавление коротко замыкает тело метода).

### 6.3 Motion Blur — velocity-driven ✅
`VisualRenderer.renderMotionBlur` переписан: теперь учитывает и угловую скорость камеры (направленные «полосы» по экранному вектору вращения), и поступательную скорость (радиальный «zoom»-виньетинг при движении вперёд). Это честный velocity-driven эффект; полноценное FBO-накопление кадров (ping-pong) требует интеграции в рендер-конвейер и в этой агентной архитектуре небезопасно без рантайм-теста.

### 6.4 ItemPhysics — ✅ реализован (предыдущая запись устарела)
**ItemPhysicsModule** добавлен (см. п. 1.19). Инъекция в `ItemEntityRenderer.method_3996` (`class_916`) через `ItemSpinAdapter`:
- В `AetherBridge`: `setItemSpinAngle(float)` / `getItemSpinAngle()` + поле `itemSpinAngle`.
- В ASM: если угол ≠ 0, вставляется `matrices.multiply(RotationAxis.POSITIVE_Y.rotation(angle))` **перед** телом рендера.
- Угол обновляется в `ItemPhysicsModule.onTick`: `(System.currentTimeMillis() / 1000f) * spinSpeed * 2π`.
- При отключении модуля: `setItemSpinAngle(0f)` — инъекция коротко замыкается на `IFEQ`.
- Юнит-тест `transformsItemEntityRendererSpinInjection` проверяет: байткод содержит `AetherBridge.getItemSpinAngle` + `MatrixStack.method_22907`; идемпотентность трансформации; рантайм-прогон с углом 0 (тело выполняется) и с углом ≠ 0 (нет short-circuit return).
Это **минимально-необходимая реализация** (только Y-спин). Полноценная физика (бобик по X/Z от скорости, отскоки, лежание плашмя) требует модификации render-state и интеграции в Fabric-сборку — оставил за пределами этого апгрейда.

---

## 7. Аудит и редизайн — третий проход (Phase 1–4)

Комплексный аудит проведён по `AUDIT_REPORT.md` (в корне staging). Ниже — реализованные фиксы.

### 7.1 Критические фиксы рендер-пайплайна (Phase 1) ✅

1. **Утечка состояния в объектном пуле** (`VisualRenderer.Pool`): пул теперь принимает `Consumer<T> reset`, который вызывается при каждом `release()`. Все 5 пулов (`JumpCircle`, `TrailPoint`, `BreadcrumbPoint`, `HitSpark`, `AmbientParticle`) сбрасывают все поля — исключён риск использования устаревших значений, если будущий код не заполнит поле при `acquire()`.
2. **GL-валидация в KawaseBlur**: добавлены проверки `sourceFbo <= 0`, проверки провала аллокации буферов (`fbos[0] == 0` после `allocateBuffers`), и `checkGLError(stage)` после каждого прохода (blit, downsample 0–2, upsample 3–1) с логом в stderr. Это устраняет «тихие» NPE/чёрные экраны при сбое FBO.
3. **Resolution-independent мержи в WorldToScreen.projectBox**: хардкод `±40f` и `bw <= 2f` заменён на проценты экрана (3% мерж, 1.5% мин. размер) — ESP-боксы теперь корректно отбрасываются на любых разрешениях.
4. **Валидация NanoVG-контекста**: `NanoVgEngine.handle()` теперь бросает `IllegalStateException` с понятным сообщением, если вызван до `ensureInit()` (вместо передачи `0L` в NanoVG, что крашило нативно). Добавлен `isInitialized()`.
5. **Scissor-стек**: проверены все push/pop-пары в `ClickGuiController` и `VisualRenderer` — пары сбалансированы; в новом коде (settings-view spring) `nvgSave`/`nvgRestore` обёрнуты корректно.

### 7.2 Редизайн экрана настроек модулей (Phase 2) ✅

1. **Spring-анимация входа/выхода**: добавлены `settingsSpring` (stiffness 280, damping 26), `settingsViewAnim`, `lastExpandedModule`. При открытии настроек модуля вид въезжает со slide-up 14px + fade + scale 0.975→1.0 (easeOutCubic поверх spring-значения). При закрытии — обратная анимация (вид рендерится fade-out, пока spring не вернётся к 0).
2. **Контекстное меню**: проверено — позиция фиксируется в момент клика (`openContextMenu(m, clickX, clickY)`), клампится в границы панели и больше не двигается. Требование «меню остаётся на месте клика» выполняется.
3. **Live-превью слайдеров**: подтверждено — `NumberSetting.visualRatio` анимируется (dt*24) и слайдер обновляет значение модуля в реальном времени через `onMouseMove` (`draggingSlider.setValue`).
4. Рендер настроек обёрнут в `nvgSave`/`nvgGlobalAlpha`/`nvgRestore` — альфа входа применяется ко всему виду (header + список + скроллбар).

### 7.3 Эстетика визуальных модулей (Phase 3) ✅

1. **Новый примитив `Render2D.drawTaperedLine`**: трапециевидная линия (толщина wStart→wEnd) с мягким под-свечением (glow-пасс ×2.6 ширины). Заливка вместо stroke — настоящее сужение.
2. **TracersModule**: трайсеры теперь сужаются к цели (wEnd = wStart × 0.32) и имеют depth-glow (alpha ×48) — читаются как «луч», а не провод.
3. **EspModule стиль «Стекло»**: добавлены glow-обод (drawGlowBox), внутренняя заливка, crisp-outline и верхний спекулярный блик (белая полоска alpha 26) — настоящий liquid-glass вид без артефактов на краях.
4. **ChamsModule — fresnel-подобная глубина**: дистанция до сущности нормализуется к maxDistance; заливка затухает с глубиной (×(1−0.45·t)), rim усиливается (alpha 200→255), glow-обод и спекулярный блик ослабевают с глубиной — имитация rim-lighting без 3D-пасса.

### 7.4 HUD-редактор: магнитное прилипание и умные направляющие (Phase 4) ✅

Полностью переписан drag-snap в `ClickGuiController`:

1. **Три уровня прилипания** (в порядке приоритета):
   - Экранные края с отступом 10px (левый/центр/правый якоря, верх/центр/низ);
   - Smart guides: выравнивание левого/центрального/правого края драгаемого виджета к краям и центрам остальных виджетов (берётся ближайшее совпадение в пределах 6px);
   - Тонкая сетка 4px как fallback, если ничто не прилипло.
2. **Умные направляющие**: активные линии выравнивания рисуются во всю ширину/высоту экрана (`snapGuideXs`/`snapGuideYs`), классические центр-линии сохранены.
3. Состояние снапов очищается при отпускании кнопки и выходе из режима редактирования.

### 7.5 Прочее

- `AUDIT_REPORT.md` — полный отчёт аудита (критические issues, приоритеты, роадмап, критерии успеха).
- Сборка и чеки после всех изменений: `:bridge:build :client:remapJar :agent:build :launcher:desktopJar :launcher:desktopTest :client:test` → **BUILD SUCCESSFUL**; `:client:checkstyleMain` → без блокирующих ошибок.

### Заметки для ревьюера (третий проход)

- `Pool<T>` в `VisualRenderer` теперь имеет два конструктора (с reset и без) — обратная совместимость сохранена.
- `drawTaperedLine` использует `MemoryStack.stackPush()` — не вызывать вне кадра NanoVG.
- Константы снапа: `SNAP_EDGE_MARGIN=10`, `SNAP_TOLERANCE=6`, `SNAP_GRID_PX=4` — можно тюнить в `ClickGuiController`.
- Smart-guides выбирают ближайшее совпадение (не последнее) — поведение детерминировано.

---

## 8. Догон третьего прохода — Tier 2 визуал + HUD-анимации

### 8.1 Ретач остальных визуальных модулей ✅

Единый liquid-glass язык (glow-обод + заливка + спекуляр) применён ко всем оставшимся оверлей-модулям:

1. **ProjectilesModule** — дуга теперь таперируется (ширина уменьшается к цели, `drawTaperedLine` с glow-пассом ×0.16), яркость привязана к энергии снаряда (гаснет по мере полёта), точки крупнее у старта; добавлен **маркер посадки** — пульсирующее кольцо + ядро в предсказанной точке падения.
2. **BreadcrumbsModule** — стиль «Лента» стал стеклянным (тапер + под-glow ×0.22), стиль «Точки» получил ореол (×2.1 радиус, alpha ×0.18) под ядром.
3. **ItemEspModule** — при `glow` рисуется полный стеклянный пакет: glow-обод, внутренняя заливка, спекулярная полоска сверху.
4. **BlockOverlayModule** — стеклянная панель поверх блока (wash + glow + спекуляр) и **Material 3 прогресс-кольцо**: тёмный фоновый трек (alpha 90), яркое заполнение, круглая «шапка» на конце дуги.
5. **HoleEspModule** — glow-обод + заливка + спекуляр поверх контура.
6. **HitTints (HitColor)** — добавлен glow-флэш, затухающий вместе с основной вспышкой (alpha ×0.5, толщина ×prog).

### 8.2 Spring-анимации входа/выхода HUD-виджетов ✅

- `HudElement` получил `enterAnim` (0..1), `updateEnterAnim(dt, visible)` (скорости 5.2 вход / 7.0 выход) и статический `enterEase(x)` — easeOutBack с мягким овершутом (c1 = 1.70158).
- `AetherClientMain` рендер-цикл: каждый виджет оборачивается в `nvgSave`/`nvgRestore`; применяется fade (alpha клампится 0..1 от овершута), slide-up 12px и scale 0.94→1.0 вокруг центра виджета. Выход — обратная анимация: виджет остаётся рендериться, пока `enterAnim > 0.002`.
- Комбинируется с существующим per-widget `elementScale` (итоговый scale = произведение).

### 8.3 Motion Blur — EMA-сглаживание ✅

- Новые поля `mbSmoothOffX/Y`, `mbSmoothStrength`; скорость камеры и углы сглаживаются экспоненциально (k = min(1, dt·14) для оффсетов, dt·10 для силы). Убирает покадровое дёргание, сохраняя кинематографичный отклик.
- Состояние сбрасывается в `reset()` и при смене мира.

### 8.4 Персистентность раскрытых настроек ✅

- `ConfigManager.saveClientConfig` пишет `"expanded"` для каждого модуля; `loadClientConfig` восстанавливает. Состояние «какой модуль открыт» переживает рестарт.

### 8.5 ToastManager — уважение настройки длительности ✅

- `push(...)` получил перегрузку с `maxAgeSec`; `pushModuleToggle(Module, NotificationsModule)` читает `duration` (ранее хардкод 3.2 с). Все 4 места вызова обновлены на передачу настроек; старый single-аргумент API сохранён для совместимости.

### Сборка

`./gradlew :bridge:build :client:remapJar :agent:build :launcher:desktopJar :launcher:desktopTest :client:test :client:checkstyleMain` → **BUILD SUCCESSFUL**.

### Осталось за пределами этого прохода (осознанно)

- **ColorPickerSetting** (HSV-колесо + альфа) — сейчас работают 6 цветных свотчей на строку; полноценный пикер требует инлайн-попапа с hue/brightness-треками. Отложено как отдельная задача.
- **Temporal Motion Blur (FBO ping-pong)** — по-прежнему вне архитектуры агента без рантайм-проверок (см. 6.3).

---

## 9. Интеграция лучших визуальных и качественных решений из внешних сурсов (`/run/media/mark/CEC64B53C64B3ACB/сурс`)

Проинспектированы исходники на внешнем носителе (`systemdlc`, `CatlavanSRC`, `Pulse`, `Rockstar`, `min4ed`, `xdModification`). Без копипаста читерских механик были извлечены и переработаны в Material 3 / Liquid Glass ключевые визуальные алгоритмы:

1. **Animated Block Outline (обновление `BlockOverlayModule` + `VisualRenderer.java`)**:
   - Плавная интерполяция (`lerp`) при переводе взгляда с блока на блок: рамка не скачет, а плавно перелетает и адаптирует размеры (`smooth_transition`, `transition_speed`).
   - Плавное затухание/появление (`smoothBlockAlpha`) при наведении на воздух или блок.
   - Формы: «Коробка», «Углы» (sci-fi угловые скобки с `corner_size`), «Свечение», «Скобка».
   - Настройки: `fill` (матовая заливка граней), `fill_alpha`, `line_width`, `through_walls` (по умолчанию `false` для чистоты перед проверками).
   - Сохранено кольцо прогресса ломания блока.

2. **Esports Dynamic Crosshair с Хитмаркерами (`CrosshairModule` + `VisualRenderer.java`)**:
   - Динамическое расширение прицела при отдаче атаки (`zoom_on_attack`, привязано к кулдауну удара игрока).
   - Анимированный хитмаркер (`hitmarker`): при нанесении удара по цели вокруг прицела на 240 мс вспыхивают 4 диагональных луча (`\ /`) с отскоком и плавным затуханием.
   - Настройки: `center_dot`, `dot_radius`, `thickness`, `size`, `gap`, стиль «Шеврон» (тактическая V-галка) + интеграция палитры/темы.

3. **Кинематографический экран (`AspectRatioModule`, новый модуль)**:
   - Модуль леттербоксинга и адаптации экрана (`21:9`, `16:9`, `4:3`, `1:1`, «Кастомный»).
   - Плавный анимированный выезд полос (`smooth_transition`, `transition_speed`).
   - Матовые границы (`frosted_borders`) со световым разделительным ободом.

4. **Голографический радар подбора (`ItemRadiusModule`, новый модуль)**:
   - Проекция пульсирующих голографических 3D-кругов на землю вокруг игрока (радиус подбора) и/или вокруг выброшенных предметов.
   - Бейдж с названием и количеством стака предметов над предметом.
   - Защита от вх: `through_walls` (по умолчанию `false`, проверка `canSee`).

5. **Баллистика снарядов и маркер приземления (`ProjectilesModule`)**:
   - Добавлен голографический маркер приземления с расходящимися волнами в точке падения жемчуга/стрелы (`landing_circle`, `landing_radius`).
   - Настройка следа частиц за летящим жемчугом (`pearl_particles`).

---

## 10. Комплексный боевой и визуальный патч (по репортам внутриигрового тестирования)

1. **Затемнение экрана в меню паузы (Esc / GameMenuScreen)**:
   - Добавлен мягкий сбалансированный затемняющий scrim (`0x44000000`) по всему экрану.
   - HUD-элементы плавно приглушают яркость до 35% прозрачности (`pauseDim = 0.35f`), чтобы интерфейс не резал глаза при открытии меню.

2. **Полноценное перемещение HUD прямо в чате (`T`) с рамками**:
   - При открытии чата активируется режим редактирования (`isChat`): все HUD-виджеты отображаются на экране (даже выключенные).
   - Отрисовывается пунктирная рамка с полупрозрачной подложкой и иконкой перетаскивания.
   - Любой элемент можно схватить мышью в чате и переместить в любое удобное место без захода в ClickGUI.

3. **Поддержка кнопок мыши в биндах (ЛКМ, ПКМ, СКМ, M4, M5, ...)**:
   - В `KeybindSetting` добавлена поддержка отрицательных кодов (`-100` для ЛКМ, `-101` для ПКМ, `-102` для СКМ, `-103` для M4, `-104` для M5 и т.д.).
   - В `ClickGuiController` клик любой кнопкой мыши по кнопке бинда корректно захватывает мышиный клик.
   - В `AetherClientMain.onMouseButton` добавлена проверка триггеров модулей по мышиным кодам.

4. **Исправление кастомного неба (Custom Sky 3D Dome)**:
   - Убран плоский 2D-градиент на весь экран в `VisualRenderer.renderCustomSky`.
   - Добавлен ASM-хук в `net.minecraft.client.render.SkyRendering` (`renderTopSky` и `renderGlowingSky`) через `AetherBridge.getCustomSkyColor`.
   - Теперь перекрашивается именно настоящий 3D-купол неба Minecraft с плавным переходом к горизонту, звёздами и северным сиянием, не заливая экран плоским градиентом.

5. **Исправление цвета урона (3D Hit Color Flash)**:
   - В `HitColorModule` стиль по умолчанию установлен в `3D Модель`.
   - При выбранном `3D Модель` убран 2D-прямоугольник в `VisualRenderer.renderHitTints`.
   - `HitColorApplier.update()` напрямую перезаписывает строки 0..7 в `OverlayTexture.NativeImage` Minecraft, окрашивая саму 3D модель игрока/моба при ударе в выбранный кастомный цвет (красный, фиолетовый, бирюзовый, градиент).

6. **Кастомные стили взмаха руки (Swing Animation Hook)**:
   - Внедрен байткод-хук в `HeldItemRenderer.applySwingOffset` (`net/minecraft/class_759`, `method_3217`).
   - Реализованы стили: «1.7 Vanilla» (классический блокхит), «Плавный» (Smooth), «Размашистый» (Swipe), «Вращение» (Twist) и «Компактный».
   - Настройки: `speed` (скорость взмаха), `angle` (наклон), `ignore_mining` (не менять при копании блоков).

7. **Кастомизация позиции и размера рук (Viewmodel Hook)**:
   - Внедрен байткод-хук в `HeldItemRenderer.applyEquipOffset` (`net/minecraft/class_759`, `method_3224`).
   - Настройки: смещение X / Y / Z, поворот Pitch / Yaw / Roll, общий масштаб предмета (`scale`), переключатель «Только правая рука».

8. **Система друзей (Friends Manager)**:
   - Реализован `FriendsManager` с автосохранением в `~/.local/share/AetherClient/friends.json`.
   - Подсветка никнеймов друзей изумрудным цветом `0xFF34D399` и тегом `[F]`.
   - Приоритет в ESP и трейсерах для союзников.
   - Игнорирование друзей в `TargetHud` (целью выбирается только противник).

9. **Быстрый свап (FastSwap / Offhand Swap)**:
   - Выбор до 2 предметов для быстрой ротации (`item1`, `item2`: Тотем, Щит, Золотое яблоко, Эндер-жемчуг, Фейерверк).
   - Мгновенный обмен с левой рукой в 1 клик по кнопке (включая мышиные клавиши) через `clickSlot(..., 40, SlotActionType.SWAP, ...)`.

10. **Синхронизация ClickPearl (ClickPearl Packet Sync)**:
    - Пакет `UpdateSelectedSlotC2SPacket` перед броском и после броска жемчуга с анимацией взмаха руки.

11. **Свободная камера (Freecam Freeze Fix)**:
    - В `FreecamModule.onTick` обнуляется `mc.player.input.playerInput` (`PlayerInput.DEFAULT`), фиксируя позицию настоящего тела игрока на земле, чтобы персонаж не взлетал при управлении камерой.

12. **Плавное следование урона за сущностью (Floating Damage Feet Tracking)**:
    - `FloatingDamage` привязан к живой сущности: числа урона плавно поднимаются вверх от ног/тела цели, перемещаясь вместе с бегущим или прыгающим противником, а не зависая на статической точке карты.

13. **Мульти-слотовая косметика (5 независимых слотов одновременно)**:
    - Голова (Нимб, Шляпа, Корона, Кошачьи ушки, Нет).
    - Тело / Крылья (Крылья: Ангельские, Демонические, Фея, Нет).
    - Руки (Митенки / Рукава, Браслеты, Нет) с двухцветной основой (белая база + акцентная кайма).
    - Ноги (Чулки, Нет) с белой базой и акцентными ажурными кружевами.
    - Аура (Орбитальные сферы, Аура пламени, Нет).
    - Переключатель «От первого лица» (`firstPerson`) с настройкой смещения по высоте.

