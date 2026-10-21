/* Service Worker：缓存静态资源，离线也能玩 */
var CACHE_NAME = 'lottery-mobile-v1';
var ASSETS = [
  './',
  'index.html',
  'css/style.css',
  'js/lottery-core.js',
  'js/app.js',
  'manifest.json',
  'icons/icon-192.png',
  'icons/icon-512.png'
];

self.addEventListener('install', function (e) {
  e.waitUntil(
    caches.open(CACHE_NAME).then(function (cache) {
      return cache.addAll(ASSETS);
    })
  );
  self.skipWaiting();
});

self.addEventListener('activate', function (e) {
  e.waitUntil(
    caches.keys().then(function (keys) {
      return Promise.all(
        keys.map(function (key) {
          if (key !== CACHE_NAME) return caches.delete(key);
        })
      );
    })
  );
  self.clients.claim();
});

self.addEventListener('fetch', function (e) {
  // 只处理 GET，缓存优先、网络兜底
  if (e.request.method !== 'GET') return;
  e.respondWith(
    caches.match(e.request).then(function (cached) {
      return cached || fetch(e.request).then(function (resp) {
        // 同源请求顺手入缓存
        if (resp && resp.ok && new URL(e.request.url).origin === location.origin) {
          var copy = resp.clone();
          caches.open(CACHE_NAME).then(function (cache) {
            cache.put(e.request, copy);
          });
        }
        return resp;
      }).catch(function () {
        // 离线且无缓存：导航请求回退到首页
        if (e.request.mode === 'navigate') {
          return caches.match('./index.html');
        }
      });
    })
  );
});
