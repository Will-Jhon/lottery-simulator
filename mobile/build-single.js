#!/usr/bin/env node
/**
 * 把 mobile/ 打包成单个 HTML 文件（CSS/JS/图标全部内联）
 * 用法：node build-single.js
 * 输出：mobile/dist/双色球模拟器.html —— 直接发给朋友，浏览器打开即玩
 */
'use strict';

const fs = require('fs');
const path = require('path');

const root = __dirname;
const distDir = path.join(root, 'dist');
const outFile = path.join(distDir, '双色球模拟器.html');

function read(rel) {
  return fs.readFileSync(path.join(root, rel), 'utf8');
}

function mustReplace(html, from, to, what) {
  if (html.indexOf(from) < 0) {
    console.error('[ERROR] 未找到待替换内容: ' + what);
    process.exit(1);
  }
  return html.replace(from, to);
}

try {
  let html = read('index.html');
  const css = read('css/style.css');
  const coreJs = read('js/lottery-core.js');
  const appJs = read('js/app.js');
  const icon = fs.readFileSync(path.join(root, 'icons/icon-192.png'));

  // 1. 内联 CSS
  html = mustReplace(html,
    '<link rel="stylesheet" href="css/style.css">',
    '<style>\n' + css + '\n</style>',
    '样式表 link 标签');

  // 2. 内联 JS（先 core 后 app，顺序不能反）
  html = mustReplace(html,
    '<script src="js/lottery-core.js"></script>',
    '<script>\n' + coreJs + '\n</script>',
    'lottery-core.js script 标签');
  html = mustReplace(html,
    '<script src="js/app.js"></script>',
    '<script>\n' + appJs + '\n</script>',
    'app.js script 标签');

  // 3. manifest / Service Worker 需要 http(s) 环境，单文件版移除
  html = mustReplace(html,
    '<link rel="manifest" href="manifest.json">',
    '<!-- PWA manifest 仅在 http(s) 下有效，单文件版已移除；直接打开也能玩 -->',
    'manifest link 标签');

  // 4. 图标转 data URI（添加到主屏幕时仍有图标）
  html = mustReplace(html,
    '<link rel="apple-touch-icon" href="icons/icon-192.png">',
    '<link rel="apple-touch-icon" href="data:image/png;base64,' + icon.toString('base64') + '">',
    'apple-touch-icon link 标签');

  // 5. 标注生成信息
  const banner = '<!-- 由 mobile/build-single.js 自动生成的单文件版，可直接发送给他人 -->';
  html = html.replace('<!DOCTYPE html>', '<!DOCTYPE html>\n' + banner);

  if (!fs.existsSync(distDir)) fs.mkdirSync(distDir);
  fs.writeFileSync(outFile, html, 'utf8');

  const kb = (fs.statSync(outFile).size / 1024).toFixed(1);
  console.log('打包完成: ' + path.relative(process.cwd(), outFile) + ' (' + kb + ' KB)');
  console.log('把这个文件通过 微信/QQ/邮件/AirDrop 发给朋友即可');
} catch (e) {
  console.error('[ERROR] ' + e.message);
  process.exit(1);
}
