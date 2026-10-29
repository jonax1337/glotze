"""Build the Glotze SVG asset pack from self-contained vector masters.

Python 3.10+; SVG generation uses only the standard library.
Optional PNG export and render verification: pip install resvg_py Pillow
"""

from __future__ import annotations

import argparse
import hashlib
import html
import io
import json
import math
from pathlib import Path
import sys
import xml.etree.ElementTree as ET
import zipfile

SOURCE = Path(__file__).resolve().parent
ROOT = SOURCE.parent
MASTER = json.loads((SOURCE / 'master.json').read_text(encoding='utf-8'))
PALETTE = MASTER['palette']
MARK = MASTER['mark']
WORD = MASTER['word']
ASSETS: list[dict] = []
ANIMATIONS: list[dict] = []


def number(value: float) -> str:
    return f'{value:.4f}'.rstrip('0').rstrip('.')


def group(content: str, x=0, y=0, scale=1) -> str:
    return f'<g transform="translate({number(x)} {number(y)}) scale({number(scale)})">{content}</g>'


def mark(variant='primary', suffix='') -> str:
    if variant == 'primary':
        return (
            f'<path id="screen{suffix}" fill="{PALETTE["lime"]}" d="{MARK["body"]}"/>'
            f'<path id="left-eye{suffix}" fill="{PALETTE["ink"]}" d="{MARK["left_eye"]}"/>'
            f'<path id="right-eye{suffix}" fill="{PALETTE["ink"]}" d="{MARK["right_eye"]}"/>'
        )
    color = PALETTE[variant]
    compound = MARK['body'] + MARK['left_eye'] + MARK['right_eye']
    return f'<path fill="{color}" fill-rule="evenodd" d="{compound}"/>'


def word(color='ink') -> str:
    return f'<path fill="{PALETTE[color]}" fill-rule="evenodd" d="{WORD["path"]}"/>'


def tagline(x, baseline, color, scale=1, small=False) -> str:
    shape = MASTER['tv_tagline' if small else 'tagline']
    path = f'<path fill="{PALETTE[color]}" fill-rule="evenodd" d="{shape["path"]}"/>'
    return group(path, x, baseline, scale)


def save_svg(relative: str, width: int, height: int, content: str, label: str, dark=False):
    path = ROOT / 'svg' / relative
    path.parent.mkdir(parents=True, exist_ok=True)
    identifier = path.stem
    desc = 'Glotze. Flat vector artwork on a transparent canvas. All lettering is outlined.'
    svg = (
        '<?xml version="1.0" encoding="UTF-8"?>\n'
        f'<svg xmlns="http://www.w3.org/2000/svg" width="{width}" height="{height}" '
        f'viewBox="0 0 {width} {height}" role="img" aria-labelledby="{identifier}-title {identifier}-desc">'
        f'<title id="{identifier}-title">{html.escape(label)}</title>'
        f'<desc id="{identifier}-desc">{desc}</desc>{content}</svg>\n'
    )
    path.write_text(svg, encoding='utf-8')
    ASSETS.append({'file': path.relative_to(ROOT).as_posix(), 'label': label, 'width': width, 'height': height, 'dark_preview': dark})


def build_vectors():
    for variant, label in [('primary', 'Color'), ('ink', 'Ink'), ('white', 'White'), ('lime', 'Lime')]:
        save_svg(f'marks/glotze-mark-{variant}.svg', 673, 504, group(mark(variant), 32, 32), f'Glotze mascot · {label}', variant in ('white', 'lime'))

    for color in ('ink', 'white', 'lime'):
        save_svg(f'wordmarks/glotze-wordmark-{color}.svg', math.ceil(WORD['width']) + 32, math.ceil(WORD['height']) + 32, group(word(color), 16, 16), f'Glotze wordmark · {color}', color in ('white', 'lime'))

    variants = [('primary', 'primary', 'ink'), ('reversed', 'primary', 'white'), ('mono-dark', 'ink', 'ink'), ('mono-light', 'white', 'white')]
    for name, mark_variant, word_color in variants:
        content = group(mark(mark_variant), 32, 53, .46) + group(word(word_color), 365, 38)
        save_svg(f'logos/glotze-logo-horizontal-{name}.svg', 1280, 320, content, f'Glotze horizontal logo · {name}', word_color == 'white')
        content = group(mark(mark_variant), 207.5, 64) + group(word(word_color), (1024-WORD['width'])/2, 548)
        save_svg(f'logos/glotze-logo-stacked-{name}.svg', 1024, 864, content, f'Glotze stacked logo · {name}', word_color == 'white')

    for variant in ('primary', 'ink', 'white'):
        content = group(mark(variant), 87, (1024 - 440 * 850 / 609) / 2, 850 / 609)
        save_svg(f'icons/glotze-app-icon-{variant}.svg', 1024, 1024, content, f'Glotze app icon · {variant}', variant == 'white')
    save_svg('icons/glotze-favicon.svg', 64, 64, group(mark(), 3, (64 - 440 * 58 / 609) / 2, 58 / 609), 'Glotze favicon')

    for color in ('ink', 'white'):
        eye_paths = MARK['left_eye'] + MARK['right_eye']
        content = group(f'<path fill="{PALETTE[color]}" d="{eye_paths}"/>', -244, -69)
        save_svg(f'marks/glotze-eyes-{color}.svg', 346, 318, content, f'Glotze eyes · {color}', color == 'white')

    for theme, text_color, muted in [('light', 'ink', 'muted_light'), ('dark', 'white', 'muted_dark')]:
        content = group(mark(), 88, 74, .65) + group(word(text_color), 560, 55, 1.12) + tagline(569, 374, muted)
        save_svg(f'banners/glotze-readme-{theme}.svg', 1600, 440, content, f'Glotze README banner · {theme}', theme == 'dark')

        content = group(mark(), 470, 40, 340 / 609) + group(word(text_color), 300, 318, 680 / WORD['width'])
        content += tagline((1280 - MASTER['tagline']['width'] * .875) / 2, 573, muted, .875)
        save_svg(f'banners/glotze-social-{theme}.svg', 1280, 640, content, f'Glotze social header · {theme}', theme == 'dark')

        content = group(mark(), 26, 44, .15) + group(word(text_color), 135, 49, .19)
        content += tagline(136, 118, muted, .8, small=True)
        save_svg(f'banners/glotze-tv-{theme}.svg', 320, 180, content, f'Glotze TV banner · {theme}', theme == 'dark')

    swatches = []
    for index, key in enumerate(('lime', 'ink', 'cream', 'white')):
        x = 32 + index * 228
        swatches.append(f'<rect x="{x}" y="24" width="196" height="104" rx="24" fill="{PALETTE[key]}"/>')
        for content, baseline in [(MASTER['palette_labels'][key]['path'], 163)]:
            swatches.append(group(f'<path fill="{PALETTE["ink"]}" fill-rule="evenodd" d="{content}"/>', x, baseline))
    save_svg('glotze-palette.svg', 944, 192, ''.join(swatches), 'Glotze color palette')

    sprite = (
        '<svg xmlns="http://www.w3.org/2000/svg">'
        f'<symbol id="glotze-mark" viewBox="0 0 673 504">{group(mark(), 32, 32)}</symbol>'
        f'<symbol id="glotze-mark-mono" viewBox="0 0 673 504"><path fill="currentColor" fill-rule="evenodd" d="{MARK["body"] + MARK["left_eye"] + MARK["right_eye"]}" transform="translate(32 32)"/></symbol>'
        f'<symbol id="glotze-wordmark" viewBox="0 0 {math.ceil(WORD["width"])+32} {math.ceil(WORD["height"])+32}"><path fill="currentColor" fill-rule="evenodd" d="{WORD["path"]}" transform="translate(16 16)"/></symbol>'
        '</svg>\n'
    )
    (ROOT / 'svg/glotze-sprite.svg').write_text(sprite, encoding='utf-8')


def build_preview():
    cards = []
    for asset in ASSETS:
        surface = 'dark' if asset['dark_preview'] else 'light'
        filename = html.escape(asset['file'])
        cards.append(f'<figure class="{surface}"><a class="art" href="{filename}"><img src="{filename}" alt="{html.escape(asset["label"])}" loading="lazy"></a><figcaption><strong>{html.escape(asset["label"])}</strong><a download href="{filename}">{filename.removeprefix("svg/")}</a><span>{asset["width"]} × {asset["height"]} · SVG · transparent</span></figcaption></figure>')
    page = '''<!doctype html>
<html lang="de"><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>Glotze · Assetpaket</title>
<style>
*{box-sizing:border-box}body{margin:0;background:#f4f3ed;color:#25282c;font:15px/1.5 "Segoe UI",system-ui,sans-serif}main{max-width:1440px;margin:auto;padding:48px 36px 72px}header{display:flex;justify-content:space-between;align-items:end;gap:32px;padding-bottom:34px;border-bottom:1px solid #d8dbd1}header img{width:min(620px,60vw);height:auto}header p{max-width:365px;margin:0;color:#63676e}.tag{display:inline-block;background:#b1f533;padding:5px 13px;border-radius:20px;font-weight:650;font-size:12px;margin-bottom:12px}nav{display:flex;gap:22px;margin:24px 0 32px}a{color:inherit}.grid{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:24px}figure{margin:0;background:white;border:1px solid #d8dbd1;border-radius:18px;overflow:hidden}.art{display:flex;align-items:center;justify-content:center;height:240px;padding:28px;background-color:#faf9f5;background-image:linear-gradient(45deg,#eceee6 25%,transparent 25%),linear-gradient(-45deg,#eceee6 25%,transparent 25%),linear-gradient(45deg,transparent 75%,#eceee6 75%),linear-gradient(-45deg,transparent 75%,#eceee6 75%);background-size:20px 20px;background-position:0 0,0 10px,10px -10px,-10px 0}.dark .art{background-color:#25282c;background-image:linear-gradient(45deg,#2c3035 25%,transparent 25%),linear-gradient(-45deg,#2c3035 25%,transparent 25%),linear-gradient(45deg,transparent 75%,#2c3035 75%),linear-gradient(-45deg,transparent 75%,#2c3035 75%)}.art img{max-width:100%;max-height:100%;object-fit:contain}figcaption{padding:18px;display:flex;flex-direction:column;gap:5px}figcaption strong{font-size:15px}figcaption a{font-size:12px;overflow-wrap:anywhere}figcaption span{font-size:12px;color:#63676e}footer{margin-top:32px;color:#63676e}@media(max-width:1000px){.grid{grid-template-columns:repeat(2,minmax(0,1fr))}}@media(max-width:620px){main{padding:26px 18px}header{display:block}header img{width:100%;margin-bottom:20px}.grid{grid-template-columns:1fr}}
</style><main><header><img src="svg/logos/glotze-logo-horizontal-primary.svg" alt="Glotze"><p><span class="tag">ASSET PACK · V1</span><br>Flache Vektoren. Transparente Zeichenflächen.<br>Alle Schriftzüge sind Pfade. Die Rasterflächen dieser Übersicht zeigen die Transparenz.</p></header><nav><a href="README.md">Verwendung</a><a href="palette.json">Farben</a><a href="manifest.json">Dateiübersicht</a></nav><section class="grid">'''
    page = page.replace('ASSET PACK · V1', f'ASSET PACK · V{MASTER["version"].rsplit(".", 1)[0]}').replace('<nav>', '<nav><a href="animations.html">Animationen</a>')
    page += ''.join(cards) + '</section><footer>Glotze · 29. Oktober 2026 · Originale SVGs über die Vorschau oder den Dateinamen öffnen.</footer></main></html>\n'
    (ROOT / 'preview.html').write_text(page, encoding='utf-8')


def transform_animation(kind, values, times, seconds, repeat='indefinite'):
    if len(values.split(';')) != len(times.split(';')):
        raise ValueError('Animation values and key times differ')
    return f'<animateTransform attributeName="transform" type="{kind}" values="{values}" keyTimes="{times}" dur="{seconds}s" repeatCount="{repeat}" fill="freeze" calcMode="linear"/>'


def moving_group(content, identifier, animation):
    return f'<g id="{identifier}">{content}{animation}</g>'


def eye(which, frames, times, seconds):
    cx, cy = (336.5, 228) if which == 'left' else (499.5, 233.5)
    shape = f'<path fill="{PALETTE["ink"]}" d="{MARK[which + "_eye"]}"/>'
    if not frames:
        return f'<g id="{which}-eye">{shape}</g>'
    inner = moving_group(group(shape, -cx, -cy), f'{which}-eye-blink', transform_animation('scale', frames, times, seconds))
    return f'<g id="{which}-eye">{group(inner, cx, cy)}</g>'


def centered_motion(content, identifier, animation):
    return group(moving_group(group(content, -304.5, -220), identifier, animation), 304.5, 220)


def build_animations():
    blink_values = '1 1;1 1;1 .07;1 1;1 1'
    blink_times = '0;.4;.42;.45;1'
    definitions = [
        ('blink', 'Blinzeln', 4.8, True, blink_values, blink_times),
        ('double-blink', 'Doppelblinzeln', 6.2, True, '1 1;1 1;1 .07;1 1;1 1;1 .07;1 1;1 1', '0;.4;.418;.44;.48;.498;.52;1'),
        ('wink', 'Zwinkern', 4.4, True, '1 1;1 1;1 .08;1 .08;1 1;1 1', '0;.35;.385;.5;.53;1'),
        ('look-around', 'Umschauen', 6.0, True, None, None),
        ('bounce', 'Hüpfen', 2.8, True, None, None),
        ('doze', 'Einschlafen', 7.2, True, '1 1;1 1;1 .28;1 .12;1 .12;1 .35;1 1', '0;.16;.38;.5;.74;.86;1'),
        ('idle', 'Ruhiger Leerlauf', 6.4, True, '1 1;1 1;1 .07;1 1;1 1', '0;.56;.585;.615;1'),
        ('appear', 'Auftauchen', .7, False, None, None),
    ]
    for name, label, seconds, loop, frames, times in definitions:
        body = f'<path id="screen" fill="{PALETTE["lime"]}" d="{MARK["body"]}"/>'
        left_frames = None if name == 'wink' else frames
        eyes = eye('left', left_frames, times, seconds) + eye('right', frames, times, seconds)
        if name == 'look-around':
            animation = transform_animation('translate', '0 0;0 0;16 -4;16 -4;-18 3;-18 3;0 0', '0;.12;.3;.44;.62;.8;1', seconds)
            eyes = moving_group(eyes, 'eye-track', animation)
        content = body + eyes
        if name == 'bounce':
            content = moving_group(content, 'mascot-bounce', transform_animation('translate', '0 0;0 0;0 -15;0 0;0 -7;0 0;0 0', '0;.2;.34;.48;.58;.67;1', seconds))
        elif name == 'idle':
            content = moving_group(content, 'mascot-idle', transform_animation('translate', '0 0;0 -3;0 0;0 2;0 0', '0;.25;.5;.75;1', seconds))
        elif name == 'doze':
            content = centered_motion(content, 'mascot-doze', transform_animation('rotate', '0;0;-3;-3;0', '0;.18;.45;.78;1', seconds))
        elif name == 'appear':
            content = centered_motion(content, 'mascot-appear', transform_animation('scale', '.82 .82;1.035 1.035;.985 .985;1 1', '0;.6;.82;1', seconds, '1'))
            content = moving_group(content, 'mascot-fade', '<animate attributeName="opacity" values="0;1;1" keyTimes="0;.5;1" dur=".7s" repeatCount="1" fill="freeze"/>')
        style = '<style>.still{display:none}@media(prefers-reduced-motion:reduce){.motion{display:none}.still{display:inline}}</style>'
        content = style + f'<g class="motion">{group(content, 32, 32)}</g><g class="still">{group(mark(suffix="-still"), 32, 32)}</g>'
        save_svg(f'animations/glotze-{name}.svg', 673, 504, content, f'Glotze · {label}')
        ASSETS[-1].update({'animated': True, 'duration_seconds': seconds, 'loop': loop, 'reduced_motion_fallback': True})
        ANIMATIONS.append({'name': name, 'label': label, 'file': f'svg/animations/glotze-{name}.svg', 'duration_seconds': seconds, 'loop': loop, 'static_fallback': 'svg/marks/glotze-mark-primary.svg'})
    for theme, text_color, muted in [('light', 'ink', 'muted_light'), ('dark', 'white', 'muted_dark')]:
        body = f'<path id="screen" fill="{PALETTE["lime"]}" d="{MARK["body"]}"/>'
        animated = body + eye('left', blink_values, blink_times, 4.8) + eye('right', blink_values, blink_times, 4.8)
        content = '<style>.still{display:none}@media(prefers-reduced-motion:reduce){.motion{display:none}.still{display:inline}}</style>'
        content += f'<g class="motion">{group(animated, 88, 74, .65)}</g><g class="still">{group(mark(suffix="-still"), 88, 74, .65)}</g>'
        content += group(word(text_color), 560, 55, 1.12) + tagline(569, 374, muted)
        save_svg(f'animations/glotze-readme-blink-{theme}.svg', 1600, 440, content, f'Glotze · Blinkender README-Banner · {theme}', theme == 'dark')
        ASSETS[-1].update({'animated': True, 'duration_seconds': 4.8, 'loop': True, 'reduced_motion_fallback': True})
        ANIMATIONS.append({'name': f'readme-blink-{theme}', 'label': f'README-Banner · {theme}', 'file': f'svg/animations/glotze-readme-blink-{theme}.svg', 'duration_seconds': 4.8, 'loop': True, 'dark_preview': theme == 'dark', 'static_fallback': f'svg/banners/glotze-readme-{theme}.svg'})
    (ROOT / 'animations.json').write_text(json.dumps(ANIMATIONS, ensure_ascii=False, indent=2)+'\n', encoding='utf-8')
    build_animation_preview()


def build_animation_preview():
    cards = []
    for item in ANIMATIONS:
        path = html.escape(item['file'])
        label = html.escape(item['label'])
        timing = f'{item["duration_seconds"]:g} s · ' + ('Schleife' if item['loop'] else 'einmalig')
        surface = 'stage dark' if item.get('dark_preview') else 'stage'
        cards.append(f'<article><div class="{surface}"><img src="{path}" data-src="{path}" data-static="{item["static_fallback"]}" alt="Glotze: {label}"></div><div class="caption"><div><h2>{label}</h2><p>{timing}</p></div><button type="button" data-replay>Erneut abspielen</button></div><a class="download" download href="{path}">SVG herunterladen</a></article>')
    page = '''<!doctype html><html lang="de"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Glotze · Animationen</title>
<style>*{box-sizing:border-box}body{margin:0;background:#f5f3eb;color:#25282c;font:15px/1.5 "Segoe UI",system-ui,sans-serif}main{max-width:1240px;margin:auto;padding:44px 32px 72px}header img{width:min(560px,100%);height:auto}header p{max-width:690px;color:#63676e}.toolbar{display:flex;align-items:center;gap:16px;flex-wrap:wrap;margin:24px 0 30px}button,.button{background:#25282c;color:white;border:0;border-radius:999px;padding:10px 16px;font:600 13px "Segoe UI",system-ui,sans-serif;cursor:pointer}.toolbar a{color:inherit}.toolbar button{background:#b1f533;color:#25282c}.grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:24px}article{border:1px solid #d9dccf;border-radius:24px;overflow:hidden;background:white}.stage{height:300px;padding:28px;background-color:#faf9f5;background-image:linear-gradient(45deg,#eef0e8 25%,transparent 25%),linear-gradient(-45deg,#eef0e8 25%,transparent 25%),linear-gradient(45deg,transparent 75%,#eef0e8 75%),linear-gradient(-45deg,transparent 75%,#eef0e8 75%);background-size:20px 20px;background-position:0 0,0 10px,10px -10px,-10px 0;display:flex;align-items:center;justify-content:center}.stage img{max-width:100%;max-height:100%;object-fit:contain}.caption{display:flex;align-items:center;justify-content:space-between;gap:12px;padding:18px 22px 8px}h1{font-size:32px;margin:18px 0 8px}h2{font-size:17px;margin:0}.caption p{font-size:12px;color:#63676e;margin:3px 0}.caption button{background:#eef0e8;color:#25282c;padding:8px 12px}.download{display:inline-block;margin:0 22px 20px;font-size:12px;color:#63676e}footer{color:#63676e;margin-top:32px;font-size:13px}@media(max-width:680px){main{padding:26px 18px}.grid{grid-template-columns:1fr}.stage{height:260px}.caption{align-items:start}.caption button{max-width:130px}}
</style><main><header><a href="preview.html"><img src="svg/logos/glotze-logo-horizontal-primary.svg" alt="Glotze"></a><h1>Ein bisschen Leben für die Glotze.</h1><p>Acht transparente SVG-Animationen zum direkten Einbauen. Die Figuren respektieren die Systemeinstellung „Bewegung reduzieren“. Die Rasterflächen gehören nur zur Vorschau.</p></header><div class="toolbar"><button type="button" id="toggle">Alle pausieren</button><button type="button" id="replay-all">Alle neu starten</button><a href="README.md#animationen">Einbauen</a><a href="preview.html">Alle Assets</a></div><section class="grid">'''
    page = page.replace('</style>', '.stage.dark{background-color:#25282c;background-image:linear-gradient(45deg,#2c3035 25%,transparent 25%),linear-gradient(-45deg,#2c3035 25%,transparent 25%),linear-gradient(45deg,transparent 75%,#2c3035 75%),linear-gradient(-45deg,transparent 75%,#2c3035 75%)}</style>')
    page = page.replace('Acht transparente SVG-Animationen zum direkten Einbauen.', 'Acht transparente Maskottchen-Animationen und zwei blinzelnde README-Banner zum direkten Einbauen.')
    page += ''.join(cards)
    page += '''</section><footer>Glotze · SVG / SMIL · Keine zusätzliche Bibliothek erforderlich. „Auftauchen“ spielt einmal; die anderen Beispiele laufen in Schleifen.</footer></main><script>
let paused=false, replay=0;
const pictures=()=>Array.from(document.querySelectorAll('.stage img'));
function start(image){image.src=image.dataset.src+'?play='+(++replay)}
function pause(image){image.src=image.dataset.static}
document.querySelector('#toggle').addEventListener('click',event=>{paused=!paused;pictures().forEach(paused?pause:start);event.currentTarget.textContent=paused?'Alle starten':'Alle pausieren'});
document.querySelector('#replay-all').addEventListener('click',()=>{paused=false;pictures().forEach(start);document.querySelector('#toggle').textContent='Alle pausieren'});
document.querySelectorAll('[data-replay]').forEach(button=>button.addEventListener('click',()=>start(button.closest('article').querySelector('img'))));
</script></html>\n'''
    (ROOT / 'animations.html').write_text(page, encoding='utf-8')


def validate_vectors():
    forbidden = {'image', 'text', 'script', 'foreignObject', 'filter', 'linearGradient', 'radialGradient'}
    for path in (ROOT / 'svg').rglob('*.svg'):
        root = ET.fromstring(path.read_text(encoding='utf-8'))
        for element in root.iter():
            local_name = element.tag.rsplit('}', 1)[-1]
            if local_name in forbidden:
                raise ValueError(f'{path.name}: forbidden element {local_name}')
            if local_name == 'style' and 'animations' not in path.parts:
                raise ValueError(f'{path.name}: styles are only used for reduced-motion fallbacks')
            if any(key.rsplit('}', 1)[-1] == 'href' for key in element.attrib):
                raise ValueError(f'{path.name}: external or embedded reference')
        ids = [element.attrib['id'] for element in root.iter() if 'id' in element.attrib]
        if len(ids) != len(set(ids)):
            raise ValueError(f'{path.name}: duplicate ids')


def render_and_validate():
    import resvg_py
    from PIL import Image

    png_dir = ROOT / 'png'
    png_dir.mkdir(exist_ok=True)
    export_paths = {
        'icons/glotze-app-icon-primary.svg',
        'logos/glotze-logo-horizontal-primary.svg',
        'logos/glotze-logo-horizontal-reversed.svg',
        'banners/glotze-readme-light.svg',
        'banners/glotze-readme-dark.svg',
        'banners/glotze-tv-dark.svg',
    }
    for asset in ASSETS:
        path = ROOT / asset['file']
        png = resvg_py.svg_to_bytes(svg_path=str(path), width=asset['width'], height=asset['height'], skip_system_fonts=True)
        image = Image.open(io.BytesIO(png)).convert('RGBA')
        alpha = image.getchannel('A')
        bounds = alpha.getbbox()
        if bounds is None or alpha.getextrema() != (0, 255):
            raise ValueError(f'{path.name}: expected visible artwork and real transparency')
        if bounds[0] <= 0 or bounds[1] <= 0 or bounds[2] >= image.width or bounds[3] >= image.height:
            raise ValueError(f'{path.name}: artwork reaches canvas edge')
        asset['render_verified'] = True
        asset['alpha_range'] = [0, 255]
        if path.relative_to(ROOT / 'svg').as_posix() in export_paths:
            (png_dir / (path.stem + '.png')).write_bytes(png)
    icon = ROOT / 'svg/icons/glotze-app-icon-primary.svg'
    for size in (16, 32, 48, 64, 128, 192, 256, 512):
        png = resvg_py.svg_to_bytes(svg_path=str(icon), width=size, height=size, skip_system_fonts=True)
        (png_dir / f'glotze-app-icon-{size}.png').write_bytes(png)
    print(f'Rendered and checked {len(ASSETS)} SVGs: transparent, visible, no clipping at the default frame.')


def write_manifest():
    content = {'brand': MASTER['brand'], 'version': MASTER['version'], 'date': MASTER['date'], 'palette': PALETTE, 'svg_files': []}
    by_file = {asset['file']: asset for asset in ASSETS}
    for path in sorted((ROOT / 'svg').rglob('*.svg')):
        rel = path.relative_to(ROOT).as_posix()
        item = dict(by_file.get(rel, {'file': rel, 'label': 'Reusable SVG symbols; import with use.'}))
        item['sha256'] = hashlib.sha256(path.read_bytes()).hexdigest()
        item['bytes'] = path.stat().st_size
        content['svg_files'].append(item)
    (ROOT / 'manifest.json').write_text(json.dumps(content, ensure_ascii=False, indent=2)+'\n', encoding='utf-8')
    (ROOT / 'palette.json').write_text(json.dumps(PALETTE, indent=2)+'\n', encoding='utf-8')
    css = ':root {\n' + ''.join(f'  --glotze-{key.replace("_", "-")}: {value};\n' for key,value in PALETTE.items()) + '}\n'
    (ROOT / 'palette.css').write_text(css, encoding='utf-8')


def archive(destination: Path):
    destination.parent.mkdir(parents=True, exist_ok=True)
    files = sorted(path for path in ROOT.rglob('*') if path.is_file() and path.suffix not in ('.zip', '.pyc') and '__pycache__' not in path.parts)
    with zipfile.ZipFile(destination, 'w', zipfile.ZIP_DEFLATED, compresslevel=9) as bundle:
        for path in files:
            bundle.write(path, 'glotze-asset-pack/' + path.relative_to(ROOT).as_posix())
    with zipfile.ZipFile(destination) as bundle:
        if bundle.testzip() is not None:
            raise ValueError('Archive integrity check failed')
    print(f'ZIP: {destination} ({len(files)} files)')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--render', action='store_true', help='Export transparent PNG fallbacks and verify SVG rendering.')
    parser.add_argument('--renderer-path', type=Path, help='Optional directory containing installed resvg_py and Pillow.')
    parser.add_argument('--archive', type=Path, help='Write an asset-pack ZIP to this path.')
    args = parser.parse_args()
    if args.renderer_path:
        sys.path.insert(0, str(args.renderer_path.resolve()))
    build_vectors()
    build_animations()
    validate_vectors()
    build_preview()
    if args.render:
        render_and_validate()
    write_manifest()
    if args.archive:
        archive(args.archive.resolve())
    print(f'Glotze asset pack: {len(ASSETS)+1} SVG files in {ROOT}')


if __name__ == '__main__':
    main()
