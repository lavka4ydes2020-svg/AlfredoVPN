# Alfredo VPN

Быстрый и простой VPN для Android на базе Xray (VLESS + Reality).

[![API](https://img.shields.io/badge/API-24%2B-yellow.svg)](https://developer.android.com/about/versions/lollipop)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.3.0-blue.svg)](https://kotlinlang.org)
[![Telegram](https://img.shields.io/badge/Telegram-@alfredovpn__info-blue.svg)](https://t.me/alfredovpn_info)

## Возможности

- **VLESS + Reality** — современный протокол с маскировкой под обычный HTTPS
- **Per-app VPN** — выберите, какие приложения идут через VPN, а какие напрямую
- **Сайты в обход VPN** — банки, госуслуги, операторы и другие российские сервисы идут напрямую (49 доменов из коробки, настраиваемый список)
- **Kill Switch** — блокирует трафик при обрыве VPN-соединения
- **Always-on VPN** — постоянное подключение, не выключается
- **Auto-reconnect** — автоматическое переподключение при смене WiFi/мобильной сети
- **QS Tile** — быстрый переключатель в шторке уведомлений
- **Индикатор скорости** — трафик в реальном времени в уведомлении
- **Тёмная тема** — система / светлая / тёмная
- **Поделиться конфигом** — QR-код и clipboard
- **Без рекламы, без регистрации, без сбора данных**

## Установка

Скачайте APK из [Releases](https://github.com/lavka4ydes2020-svg/AlfredoVPN/releases) или соберите из исходников.

### Сборка

```bash
git clone https://github.com/lavka4ydes2020-svg/AlfredoVPN.git
cd AlfredoVPN/V2rayNG
./gradlew assemblePlaystoreDebug
```

APK: `app/build/outputs/apk/playstore/debug/`

## Разработка

Проект — форк [v2rayNG](https://github.com/2dust/v2rayNG) с ребрендингом и дополнительными функциями.

- Application ID: `com.alfredovpn`
- URL-схема: `alfredovpn://`
- Минимальная версия Android: 7.0 (API 24)

## Контакты

- Telegram: [@alfredovpn_info](https://t.me/alfredovpn_info)
- GitHub Issues: [сообщить о проблеме](https://github.com/lavka4ydes2020-svg/AlfredoVPN/issues)
