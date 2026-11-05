const fs = require('node:fs');
const path = require('node:path');
const { pathToFileURL } = require('node:url');

const out = __dirname;
const repo = path.resolve(out, '../../..');
const data = (p, mime) => `data:${mime};base64,${fs.readFileSync(p).toString('base64')}`;
const asset = p => data(path.join(out, 'assets', p), 'image/png');
const font = p => data(path.join(repo, 'app/src/main/res/font', p), p.endsWith('.otf') ? 'font/otf' : 'font/ttf');
const illustrations = ['01-moti.png', '02-dash-v3.png', '03-friends.png'].map(asset);
const stk = data(path.join(repo, 'app/src/main/res/drawable-nodpi/motorica_stk_card_background.png'), 'image/png');
const names = ['Моти крупным планом', 'Погоня · смещение влево', 'Кошачья команда'];
const descriptions = ['Главный герой узнаётся с первого взгляда.', 'Исходный масштаб. Сдвиг влево, фон продолжен справа.', 'Три характера и настроение коллекции.'];
const trash = '<svg width="20" height="20" viewBox="0 0 20 20" fill="none" stroke="#FF453A" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M3.125 4.375h13.75" stroke-width="2"/><path d="M8.125 8.125v5m3.75-5v5M15.625 4.375V16.25a.625.625 0 0 1-.625.625H5a.625.625 0 0 1-.625-.625V4.375M13.125 4.375v-1.25a1.25 1.25 0 0 0-1.25-1.25h-3.75a1.25 1.25 0 0 0-1.25 1.25v1.25"/></svg>';
const arrow = '<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"><path d="m13 5-7 7 7 7M6 12h15"/></svg>';
const battery = '<svg width="34" height="34" viewBox="0 0 36 36"><circle cx="18" cy="18" r="15" fill="none" stroke="#444" stroke-width="2.5"/><circle cx="18" cy="18" r="15" fill="none" stroke="#CAF35A" stroke-width="2.5" stroke-dasharray="87.65 94.25" stroke-linecap="round" transform="rotate(-90 18 18)"/><path d="m19.3 10-6 9h4l-1 7 6.4-10H18z" fill="#CAF35A"/></svg>';

function card(title, background, extra = '') {
  return `<div class="card ${extra}"><img class="card-art" src="${background}" alt="" decoding="sync"><div class="fade"></div><div class="card-content"><div class="copy"><div class="game-title">${title}</div><div class="game-status">Установлена</div></div><div class="actions"><button class="play" tabindex="-1">Играть</button><button class="delete" aria-label="Удалить" tabindex="-1">${trash}</button></div></div></div>`;
}

function screen(i) {
  return `<div class="phone" id="screen-${i + 1}" aria-label="Экран игр MS. Вариант ${i + 1}"><div class="systembar"><span>15:35</span><div class="system-icons"><svg width="17" height="15" viewBox="0 0 17 15" fill="currentColor"><path d="M0 13h2v2H0zm4-4h2v6H4zm4-4h2v10H8zm4-4h2v14h-2z"/></svg><svg width="18" height="15" viewBox="0 0 20 16" fill="none" stroke="currentColor" stroke-width="2.2"><path d="M2 5c5-4 11-4 16 0M5 9c3-3 7-3 10 0m-7 3c1-1 3-1 4 0"/><circle cx="10" cy="15" r="1" fill="currentColor" stroke="none"/></svg><svg width="22" height="13" viewBox="0 0 24 14"><rect x=".5" y="1" width="20" height="12" rx="2" fill="none" stroke="currentColor"/><rect x="2.5" y="3" width="16" height="8" rx=".8" fill="currentColor"/><path d="M22 5v4" stroke="currentColor" stroke-width="2"/></svg></div></div><div class="appbar"><div class="back">${arrow}</div><div class="device">00001<span class="connected"></span></div><div class="battery">${battery}</div></div><div class="divider"></div><div class="games-list">${card('Fluxara Drift', stk)}${card('Sushi Dash', illustrations[i], 'sushi')}</div><div class="system-navigation"><span></span></div></div>`;
}

const html = `<!doctype html><html lang="ru"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1"><title>Sushi Dash — три подложки в экране игр MS</title><style>
@font-face{font-family:Inter;src:url('${font('font_inter_semi_bold.ttf')}');font-weight:600}
@font-face{font-family:OpenSans;src:url('${font('font_open_sans_regular.ttf')}');font-weight:400}
@font-face{font-family:OpenSans;src:url('${font('font_open_sans_semi_bold.ttf')}');font-weight:600}
@font-face{font-family:SFDisplay;src:url('${font('sf_pro_display_light.otf')}');font-weight:300}
*{box-sizing:border-box}html{background:#141719;color:#fcfcfc;font-family:OpenSans,sans-serif}body{margin:0;padding:48px 52px 36px}.page{width:1242px;margin:auto}.eyebrow{font:600 12px Inter,sans-serif;letter-spacing:.13em;color:#b0b9b6;text-transform:uppercase}.heading{font:600 34px/1.25 Inter,sans-serif;margin:14px 0 12px;letter-spacing:-.035em}.intro{font-size:14px;line-height:1.7;color:#a0aaa6;margin:0 0 35px}.grid{display:grid;grid-template-columns:repeat(3,390px);gap:36px}.variant-head{min-height:87px}.variant-title{display:flex;gap:10px;align-items:center;font:600 16px Inter,sans-serif}.index{font-size:12px;color:#caf35a;border:1px solid #3f4931;background:#20271d;padding:6px 7px;border-radius:7px}.description{font-size:12px;color:#a0aaa6;margin:10px 0 0 40px;line-height:1.5}.phone{position:relative;width:390px;height:844px;background:#2a2a2a;overflow:hidden;border-radius:24px;border:1px solid #494d4a;box-shadow:0 14px 40px #0003}.systembar{height:28px;padding:7px 18px 0;font:600 12px Inter,sans-serif;display:flex;justify-content:space-between;align-items:start}.system-icons{display:flex;align-items:center;gap:5px;height:16px}.appbar{height:50px;position:relative;padding:8px;display:flex;align-items:center}.back{position:absolute;left:18px;top:8px;width:34px;height:34px;border-radius:50%;background:#373737;border:1px solid #444;display:grid;place-items:center}.device{position:absolute;left:50%;top:0;height:50px;transform:translateX(-50%);display:flex;align-items:center;font:600 18px Inter,sans-serif}.connected{position:absolute;left:calc(100% + 16px);width:12px;height:12px;background:#caf35a;border-radius:50%}.battery{position:absolute;right:18px;top:8px;width:34px;height:34px}.divider{height:1px;background:#444;margin-bottom:8px}.games-list{padding:24px 16px}.card{position:relative;height:124px;width:358px;overflow:hidden;border-radius:12px;border:1px solid #444;margin-bottom:12px;background:#373737;box-shadow:0 2px 3px #0002}.card-art{position:absolute;inset:0;width:100%;height:100%;object-fit:cover;object-position:center}.fade{position:absolute;inset:0;background:#000000b3}.card-content{position:absolute;inset:0;padding:16px;display:flex;align-items:flex-start;gap:12px}.copy{flex:1;min-width:0}.game-title{font:600 18px/22px Inter,sans-serif;color:#fcfcfc}.game-status{font:400 14px/20px OpenSans,sans-serif;color:#838383;margin-top:6px}.actions{display:flex;gap:8px;flex:0 0 162px}.actions button{height:42px;flex-shrink:0;border:1px solid #444;border-radius:12px;background:#2a2a2a;color:#fff;box-shadow:0 2px 4px #0005;padding:0}.play{width:112px;font:300 12px SFDisplay,sans-serif}.delete{width:42px;display:grid;place-items:center}.system-navigation{position:absolute;bottom:0;left:0;width:100%;height:25px;display:flex;justify-content:center;align-items:center}.system-navigation span{width:108px;height:4px;border-radius:3px;background:#eee}.closeup{margin-top:24px}.closeup-label{font:600 11px Inter,sans-serif;letter-spacing:.04em;color:#8f9b95;margin:0 0 10px}.closeup .card{width:390px;height:135px}.closeup .card-content{transform:scale(1.09);transform-origin:top left;width:358px;height:124px}.closeup .card-art{object-position:center}.cards-only .phone{display:none}.cards-only .closeup{margin-top:0}.cards-only .variant-head{min-height:87px}.footer{font-size:11px;line-height:1.6;color:#738078;margin:26px 0 0}.single .phone{border:0;border-radius:0;box-shadow:none}.single{padding:0!important;background:#2a2a2a}.single .page{width:390px}.single .eyebrow,.single .heading,.single .intro,.single .variant-head,.single .closeup,.single .footer{display:none}.single .grid{display:block}.single .variant{display:none}.single[data-variant="1"] .variant:nth-child(1),.single[data-variant="2"] .variant:nth-child(2),.single[data-variant="3"] .variant:nth-child(3){display:block}@media(max-width:1345px){body:not(.single){padding:32px 20px}.page{max-width:100%}.grid{grid-template-columns:repeat(3,390px);width:max-content}.page{overflow-x:auto}.heading{font-size:28px}}
</style></head><body><main class="page"><div class="eyebrow">Motorica Start · Sushi Dash</div><h1 class="heading">Три настроения одной карточки</h1><p class="intro">Варианты в текущем Android-экране MS: штатные шрифты, текст и кнопки. Подложка под чёрным фейдом 70%.</p><div class="grid">${names.map((name, i) => `<section class="variant"><div class="variant-head"><div class="variant-title"><span class="index">0${i + 1}</span>${name}</div><p class="description">${descriptions[i]}</p></div>${screen(i)}<div class="closeup"><p class="closeup-label">SUSHI DASH · КАРТОЧКА КРУПНЕЕ</p>${card('Sushi Dash', illustrations[i], 'sushi')}</div></section>`).join('')}</div><p class="footer">Макеты для выбора иллюстрации. Состояние игр — установлены. Пропорции карточки и фейд сохранены из текущего Android-интерфейса.</p></main><script>const v=new URLSearchParams(location.search).get('variant');if(v){document.body.classList.add('single');document.body.dataset.variant=v}if(new URLSearchParams(location.search).has('cards'))document.body.classList.add('cards-only')</script></body></html>`;

fs.writeFileSync(path.join(out, 'preview.html'), html);
console.log('Wrote preview.html with embedded images and fonts.');

async function waitForAssets(page) {
  await page.evaluate(async () => {
    await document.fonts.ready;
    await Promise.all(Array.from(document.images, async img => {
      await img.decode();
      if(!img.naturalWidth || !img.naturalHeight) throw new Error('Image failed to decode');
    }));
    await new Promise(resolve => requestAnimationFrame(() => requestAnimationFrame(resolve)));
  });
}

if (!process.argv.includes('--build-only')) {
  (async () => {
    const {chromium} = require('/Applications/ChatGPT.app/Contents/Resources/cua_node/lib/node_modules/playwright');
    const browser = await chromium.launch({headless:true, executablePath:'/Applications/Yandex.app/Contents/MacOS/Yandex'});
    const page = await browser.newPage({viewport:{width:1346,height:1400},deviceScaleFactor:2});
    await page.goto(pathToFileURL(path.join(out, 'preview.html')).href);
    await waitForAssets(page);
    await page.screenshot({path:path.join(out,'comparison.png'),fullPage:true});
    for(let i=1;i<=3;i++) {
      await page.goto(pathToFileURL(path.join(out,'preview.html')).href+`?variant=${i}`);
      await waitForAssets(page);
      await page.locator(`#screen-${i}`).screenshot({path:path.join(out,`screen-0${i}.png`)});
      await page.locator(`#screen-${i} .sushi`).screenshot({path:path.join(out,`card-0${i}.png`)});
      if(i===2) {
        await page.locator('#screen-2').screenshot({path:path.join(out,'selected-screen.png')});
        await page.locator('#screen-2 .sushi').screenshot({path:path.join(out,'selected-card.png')});
      }
    }
    await page.setViewportSize({width:1346,height:470});
    await page.goto(pathToFileURL(path.join(out,'preview.html')).href+'?cards=1');
    await waitForAssets(page);
    await page.screenshot({path:path.join(out,'cards-comparison.png'),fullPage:true});
    const overflow = await page.locator('.card').evaluateAll(cards => cards.map(c => ({title:c.querySelector('.game-title').textContent,width:c.getBoundingClientRect().width,height:c.getBoundingClientRect().height})).slice(-3));
    console.log(JSON.stringify({exported:['comparison.png','screen-01.png','screen-02.png','screen-03.png','card-01.png','card-02.png','card-03.png','cards-comparison.png','selected-screen.png','selected-card.png'],cardGeometry:overflow}));
    await browser.close();
  })().catch(e=>{console.error(e);process.exitCode=1});
}
