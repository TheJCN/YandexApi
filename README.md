# 📦 YandexApi

YandexApi — это Java‑клиент для взаимодействия с неофициальным API Яндекс.Музыки.  
Позволяет асинхронно получать **ID текущего трека**, а также основную информацию о нём (название, артист и т.д.).

## ✨ Возможности
- Получение ID текущего трека (`getCurrentTrackIdAsync`)
- Получение информации о треке (`getTrackInfoAsync`)
- Простое подключение через `OkHttp` + `Jackson`
- Поддержка асинхронного взаимодействия (`CompletableFuture`)

## 🔧 Использование

```java
YandexMusicClient client = new YandexMusicClient();
client.setToken("ваш_токен");

String trackId = client.getCurrentTrackIdAsync().join();
TrackInfo info = client.getTrackInfoAsync(trackId).join();

System.out.println(info.getTitle() + " - " + info.getArtist());
```

## 🔑 Получение токена для API
- Для получения токена ознакомьтесь с этой статьей: [Получение токена](https://yandex-music.readthedocs.io/en/main/token.html)

# 📦 Подключение через JitPack
```gradle
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.TheJCN:YandexApi:1.0.0'
}
```

```maven
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependency>
    <groupId>com.github.TheJCN</groupId>
    <artifactId>YandexApi</artifactId>
    <version>4a55c77891</version>
</dependency>
```

[![](https://jitpack.io/v/TheJCN/YandexApi.svg)](https://jitpack.io/#TheJCN/YandexApi)
