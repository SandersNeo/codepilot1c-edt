# CodePilot1C (EDT plugin)

Открытая (OSS) часть плагина для 1C:EDT на базе Eclipse RCP/OSGi.

## Сборка

Требования: JDK 17.

```bash
mvn -B -V --no-transfer-progress clean verify
```

## Структура

- `bundles/` — OSGi плагины
- `features/` — Eclipse features
- `repositories/` — p2 update site
- `targets/` — target platform
