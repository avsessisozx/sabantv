# DEVİR NOTU (yapay zeka / geliştirici için)

Bu dosya, projeyi devralan bir yapay zekanın veya kişinin sıfırdan başlamadan devam edebilmesi için yazıldı.
Son güncelleme: 10 Ekim 2026.

## Proje nedir?

CloudStream (Android medya oynatıcı) için **Türkçe eklenti deposu**. Sahibi yazılımcı değil; işi sen yürüteceksin,
kullanıcı telefonda test edip sonucu bildiriyor.

- Depo: https://github.com/avsessisozx/sabantv (herkese açık, `recloudstream/TestPlugins` şablonundan fork)
- CloudStream'e eklenecek depo adresi: `https://raw.githubusercontent.com/avsessisozx/sabantv/master/repo.json`
- Şu an tek eklenti: **AvsarFilm** (Avşar Film'in resmi YouTube kanalı, kanal ID: `UCcpY8WNM1xHPb8t2rjjZmwg`)

## Kullanıcının kuralı: yalnızca yasal / resmi kaynaklar

Kullanıcı başta lisanssız film-dizi sitelerini (ör. hdfilmcehennemi, webteizle) eklemek istedi; bunlar için kod yazılmadı.
Yalnızca resmi/yasal kaynaklar eklenir: yapımcı veya kanalların kendi resmi YouTube kanalları, TRT gibi kamu yayıncısı,
serbest lisanslı arşivler. Yeni kaynağı eklemeden önce resmi olup olmadığını kontrol et (kendi web sitesi, doğrulama, içerik sahibi mi).

## Dosya yapısı

- `repo.json` : CloudStream'in depoyu tanıdığı manifest; `builds` dalındaki `plugins.json`'a işaret eder.
- `build.gradle.kts` (kök) : Gradle ayarları. Kotlin **2.4.0** (CloudStream kütüphanesiyle uyumlu olmak zorunda).
- `AvsarFilm/` : eklenti modülü (her eklenti ayrı klasör; `settings.gradle.kts` klasörleri otomatik dahil eder).
  - `build.gradle.kts` : sürüm (`version = 1`), açıklama, dil, tür. Eklentiyi değiştirince `version`'ı 1 artır.
  - `src/main/kotlin/com/example/AvsarFilm.kt` : sağlayıcı (liste, arama, detay, oynatma)
  - `src/main/kotlin/com/example/AvsarFilmPlugin.kt` : eklentiyi kaydeder
- `.github/workflows/build.yml` : GitHub Actions; `master`'a push olunca derler, çıktıyı `builds` dalına yazar.
- `builds` dalı : **otomatik üretilir, elle düzenleme.** İçinde `.cs3`, `plugins.json` ve `build.log` olur.

## Link yapıştırarak yeni site ekleme (otomatik)

`.github/workflows/kaynak-ekle.yml` ("Yeni Kaynak Ekle"): GitHub'da Actions → Yeni Kaynak Ekle → Run workflow → link yapıştır.
Claude (claude-code-action) önce yasallık kontrolü yapar, uygunsa `AvsarFilm/` yapısını örnek alıp siteye özel modül yazar,
`./gradlew <Modul>:make` ile derler ve sonucu `KAYNAK_SONUCU.md` dosyasına yazar. Sonraki adım dalı (`kaynak-<run_id>`) yükleyip PR açar.
Kullanıcı PR'ı birleştirince `Build` akışı eklentiyi yayınlar.
- Gerekli: depo secret'ı `ANTHROPIC_API_KEY` ve depo ayarında "Allow GitHub Actions to create and approve pull requests".
- PR'lar GITHUB_TOKEN ile açıldığı için PR üzerinde derleme akışı tetiklenmez; derleme sonucu PR açıklamasında yazar.

## Yeni bir resmi YouTube kanalı nasıl eklenir?

1. Kullanıcıdan kanalın bağlantısını al (`youtube.com/@isim` veya `/channel/UC...`).
2. Kanal ID'sini (UC ile başlayan 24 karakter) bul; kanalın sayfasında ya da "Hakkında" bölümünde bulunur.
3. Kanalın resmi olduğunu doğrula (yukarıdaki kural).
4. `AvsarFilm/` klasörünü örnek alıp yeni bir modül aç (veya mevcut yapıyı birden fazla kanalı destekleyecek şekilde genelleştir).
   Değişen yerler: klasör ve sınıf adları, `name`, `channelId`, `build.gradle.kts` içindeki açıklama.
5. Push et, derlemeyi izle (aşağıya bak), kullanıcıya telefonda denetmek için net adımlar ver.

Video listesi kanalın RSS beslemesinden gelir: `https://www.youtube.com/feeds/videos.xml?channel_id=<ID>`.
Oynatma CloudStream'in yerleşik YouTube çıkartıcısına (`loadExtractor`) bırakılır.

## Bilinen sınırlar / yapılacaklar

- RSS beslemesi yalnızca **son ~15 videoyu** verir; aramada da yalnızca bunlar aranır. Tüm katalog için kanal sayfası/oynatma listelerini
  ayrıştırmak gerekir (daha kırılgan, test edilmeli).
- **Doğrulanmadı:** eklentinin telefonda gerçekten liste gösterdiği ve videoyu oynattığı. Derleme başarılı, ama çalışma testi kullanıcıdan bekleniyor.
- RSS ayrıştırma mantığı yalnızca örnek veriyle denendi, gerçek kanal verisiyle değil.

## Derleme ve hata ayıklama (önemli ipuçları)

- Derleme `master`'a push ile başlar (`*.md` değişiklikleri derlemeyi tetiklemez). Çalıştırma durumu: `gh run list -R avsessisozx/sabantv`.
- GitHub Actions kayıtlarının tamamı dışarıdan okunamayabilir. Bu yüzden akış tüm Gradle çıktısını `builds` dalına `build.log` olarak yazar.
  Hata olursa oradan oku: `git fetch --depth=1 origin +builds:refs/remotes/origin/builds` sonra `git show origin/builds:build.log`.
  **`+` işareti şart:** `builds` dalı her derlemede zorla üzerine yazıldığı için onsuz eski kopyayı okursun (bu hata yaşandı).
- Kotlin/AGP sürüm kayması en olası kırılma sebebidir. Hata `incompatible version of Kotlin` diyorsa CloudStream'in güncel
  `gradle/libs.versions.toml` dosyasına bak ve `kotlin-gradle-plugin` sürümünü ona yükselt.
- Android SDK için ayrı action kullanılmaz; GitHub imajındaki hazır SDK yeterli (ayrı action hata vermişti).
- Her push'tan sonra derlemenin sonucunu **kendin doğrula**; yeşil tik görmeden "çalışıyor" deme. Bir de `builds` dalındaki
  `plugins.json` içindeki `fileHash`/`fileSize` değerlerinin `.cs3` dosyasıyla tuttuğunu kontrol et.

## Kullanıcıya iletişim

Kullanıcı yazılımcı değil, Türkçe konuşuyor. Teknik terimleri sade anlat, ne yapması gerektiğini adım adım yaz.
Yapamadığın veya doğrulayamadığın şeyi açıkça söyle; "çalışıyor" ancak telefonda denendikten sonra denir.
