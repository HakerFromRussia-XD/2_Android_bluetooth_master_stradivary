from pathlib import Path
from io import BytesIO
from datetime import datetime
from zoneinfo import ZoneInfo
import hashlib
import json
import subprocess
from docx import Document
from docx.shared import Inches, Pt, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.table import WD_TABLE_ALIGNMENT, WD_CELL_VERTICAL_ALIGNMENT
from docx.oxml import OxmlElement
from docx.oxml.ns import qn
from docx.opc.constants import RELATIONSHIP_TYPE as RT
from PIL import Image, ImageOps

OUT = Path(__file__).resolve().parent
ROOT = OUT.parents[2]
E = json.loads((OUT/'evidence.json').read_text())
DOC = Document()
SEC = DOC.sections[0]
SEC.page_width = Inches(8.27)
SEC.page_height = Inches(11.69)
SEC.top_margin = SEC.bottom_margin = Inches(.65)
SEC.left_margin = SEC.right_margin = Inches(.7)
SEC.footer_distance = Inches(.3)
for name in ['Normal', 'Title', 'Subtitle', 'Heading 1', 'Heading 2', 'Caption']:
    s=DOC.styles[name]
    s.font.name='Arial'
    s.font.color.rgb=RGBColor(0,0,0)
    s.paragraph_format.space_after=Pt(8)
    s.paragraph_format.line_spacing=1.13
DOC.styles['Normal'].font.size=Pt(10.5)
DOC.styles['Title'].font.size=Pt(25)
DOC.styles['Heading 1'].font.size=Pt(20)
DOC.styles['Heading 2'].font.size=Pt(13)
DOC.styles['Caption'].font.size=Pt(8.5)
DOC.styles['Caption'].font.italic=False
for name in ['Title','Subtitle','Heading 1','Heading 2']:
    DOC.styles[name]._element.get_or_add_pPr().remove(DOC.styles[name]._element.get_or_add_pPr().find(qn('w:pBdr'))) if DOC.styles[name]._element.get_or_add_pPr().find(qn('w:pBdr')) is not None else None
DOC.core_properties.title='Отчёт об изменениях Motorica Start и Fluxara Drift за неделю'
DOC.core_properties.subject='28 сентября — 4 октября 2026'
DOC.core_properties.author=''
DOC.core_properties.keywords='Motorica Start, MS, Fluxara Drift, weekly, commits'

def para(text='',style=None,bold=False):
    p=DOC.add_paragraph(style=style)
    r=p.add_run(text)
    r.bold=bold
    return p

def heading(text,level=1):
    return DOC.add_heading(text,level)

def newpage(title):
    DOC.add_page_break()
    heading(title)

def link(p,label,url):
    h=OxmlElement('w:hyperlink')
    h.set(qn('r:id'),p.part.relate_to(url,RT.HYPERLINK,is_external=True))
    r=OxmlElement('w:r')
    prop=OxmlElement('w:rPr')
    color=OxmlElement('w:color');color.set(qn('w:val'),'155B91');prop.append(color)
    r.append(prop)
    t=OxmlElement('w:t');t.text=label;r.append(t);h.append(r);p._p.append(h)

def commits(*ids):
    p=para('Коммиты: ',style='Caption')
    for i,cid in enumerate(ids):
        c=next(c for c in E['ms'] if c['sha'].startswith(cid))
        if i:p.add_run(' · ')
        link(p,cid,c['url'])

def table(headers,rows,widths):
    t=DOC.add_table(rows=1,cols=len(headers))
    t.alignment=WD_TABLE_ALIGNMENT.CENTER;t.autofit=False
    for i,w in enumerate(widths):t.columns[i].width=Inches(w)
    for i,h in enumerate(headers):t.rows[0].cells[i].text=h
    for row in rows:
        cells=t.add_row().cells
        for i,v in enumerate(row):cells[i].text=str(v)
    for ri,row in enumerate(t.rows):
        trPr=row._tr.get_or_add_trPr()
        cant=OxmlElement('w:cantSplit');trPr.append(cant)
        if ri==0:trPr.append(OxmlElement('w:tblHeader'))
        for ci,cell in enumerate(row.cells):
            cell.width=Inches(widths[ci]);cell.vertical_alignment=WD_CELL_VERTICAL_ALIGNMENT.CENTER
            pr=cell._tc.get_or_add_tcPr()
            shade=OxmlElement('w:shd');shade.set(qn('w:fill'),'163B57' if ri==0 else ('F1F5F8' if ri%2 else 'FFFFFF'));pr.append(shade)
            borders=OxmlElement('w:tcBorders')
            for side in ['top','left','bottom','right']:
                b=OxmlElement('w:'+side);b.set(qn('w:val'),'single');b.set(qn('w:sz'),'4');b.set(qn('w:color'),'D9D9D9');borders.append(b)
            pr.append(borders)
            margins=OxmlElement('w:tcMar')
            for side in ['top','bottom','left','right']:
                m=OxmlElement('w:'+side);m.set(qn('w:w'),'100');m.set(qn('w:type'),'dxa');margins.append(m)
            pr.append(margins)
            for p in cell.paragraphs:
                p.paragraph_format.space_after=Pt(2);p.paragraph_format.space_before=Pt(2)
                p.paragraph_format.line_spacing=1.07
                if ci==0:p.alignment=WD_ALIGN_PARAGRAPH.CENTER
                for r in p.runs:
                    r.font.size=Pt(9)
                    if ri==0:r.bold=True;r.font.color.rgb=RGBColor(255,255,255)
    para().paragraph_format.space_after=Pt(0)
    return t

ASSETS=[]
def image_data(path,commit='a6a4696df',transpose=True):
    raw=subprocess.check_output(['git','show',commit+':'+path],cwd=ROOT)
    original=Image.open(BytesIO(raw))
    img=ImageOps.exif_transpose(original) if transpose else original.copy()
    img=img.convert('RGB')
    buf=BytesIO();img.save(buf,format='PNG');buf.seek(0)
    ASSETS.append({'path':path,'commit':commit,'sha256':hashlib.sha256(raw).hexdigest(),'dimensions':original.size,'exif_orientation':original.getexif().get(274),'presentation':'EXIF orientation respected; full frame preserved' if transpose else 'native pixels'})
    return buf

def picture(path,width,caption,commit='a6a4696df'):
    p=para();p.alignment=WD_ALIGN_PARAGRAPH.CENTER
    shape=p.add_run().add_picture(image_data(path,commit),width=Inches(width))
    shape._inline.docPr.set('descr',caption)
    p.paragraph_format.space_after=Pt(4)
    para(caption,'Caption')

F='output/fluxara-ios-ms-control/'
I='output/instruction-localization/'

para('Изменения Motorica Start и Fluxara Drift за неделю','Title')
para('28 сентября — 4 октября 2026','Subtitle')
para('За неделю в Motorica Start улучшили пользовательское обновление прошивки V3, переработали помощь по расширенным настройкам и перевели запуск гоночной игры на идентификаторы Fluxara Drift. Параллельно в отдельной ветке перенесли значительную часть логики V3 из Android в общий модуль KMM.')
para('В истории MS найдено 10 уникальных коммитов. В трёх ветках репозитория Fluxara Drift новых коммитов за этот период нет. Визуальный результат работы над связкой приложений сохранён в MS: скриншоты iPhone, материалы HUD и отчёты проверки вошли в коммит от 2 октября.')
table(['Показатель','Результат недели'],[
    ['10','Уникальных коммитов MS в двух активных ветках'],
    ['8 + 2','Обычных коммитов и коммитов слияния'],
    ['186','Файлов затронул перенос логики V3 в shared'],
    ['0','Коммитов Fluxara Drift за выбранную неделю'],
],[1.2,5.65])
picture(F+'figma-left-gauge-aligned-pause.png',6.65,'HUD Fluxara Drift с дуговыми индикаторами у руля. Сохранённый макет Figma из коммита MS a6a4696df; это дизайн, а не кадр работающей игры.')
para('Главные результаты','Heading 2')
para('Для пользователя — более цельный сценарий обновления и объяснения настроек на русском и английском. Для разработки — общая логика V3 и согласованные параметры связи MS с Fluxara Drift.')

newpage('Обновление V3 и общий модуль KMM')
heading('Обновление прошивки проходит одной очередью',2)
para('28 сентября исправили переход к следующей плате после успешной передачи. Подтверждение GOOD_CRC теперь сохраняет завершённый шаг сразу, без ожидания нового BLE advertisement. Журнал получил версию формата 2; старые журналы, способные остановить продолжение обновления, сбрасываются.')
para('Для пользователя внутренние проверки и переподключения отображаются в рамках одного диалога обновления. Для диагностики добавлены сообщения USER_DFU о версиях плат, передачах и ошибках. После слияния также исправлены импорты диалога и добавлен UI-тест его открытия.')
commits('93803ef4b','68626851a','aee8e4a97')
heading('Логика V3 стала общей для платформ',2)
para('29 сентября завершили следующий этап рефакторинга нативного Android. 2 октября перенесли в shared сценарии и общие репозитории для настроек, датчиков, жестов, калибровки, данных устройства, профилей аккаунта, статистики, телеметрии и пользовательской прошивки. Платформенные настройки и системные операции остались в Android.')
table(['Метрика коммита 80a0387b8','Значение'],[
    ['Затронутые файлы','186'],
    ['Файлы со статусом renamed в GitHub','125'],
    ['Затронутые файлы внутри shared','134'],
    ['Добавлено / удалено строк','1440 / 302'],
],[4.6,2.25])
para('Практический смысл: правила изменения настроек и состояния устройства теперь можно использовать из общего модуля. Сам перенос ещё не подтверждает, что весь iOS-интерфейс подключён к этим сценариям. В диффе обновлены архитектурные и функциональные тесты; результаты их запуска здесь не утверждаются.')
commits('132f05eb9','11792adbc','80a0387b8')
para('В коммите перед слиянием также убраны пользовательские toast-сообщения фоновой телеметрии: события продолжают попадать в журнал, но не отвлекают пользователя.')

newpage('Помощь по настройкам на русском и английском')
para('Вместо восьми больших изображений с готовым текстом страница получила нативные заголовки и описания, а изображения остались иллюстрациями виджетов. Добавлены русские и английские ресурсы, подбор изображения по локали и единые параметры типографики.')
para('Раздел объясняет переключение жестов датчиками, блокировку движения по EMG, тайм-аут экрана, чувствительность, силу и скорость, режимы управления и действие при смене жеста. В Android и iOS исправлены отступы; в легенде датчиков уточнены порядок и цвета индикаторов.')
commits('12d54abb0','a6a4696df')
picture(I+'previous-checkout/widgets-final.png',4.85,'Русские и английские иллюстрации восьми виджетов. Архивная подборка из previous-checkout, включённая в 12d54abb0; показывает материалы переноса, а не новую runtime-проверку текущей версии.',commit='12d54abb0')
para('В сохранённом отчёте сборки текущего checkout для iOS Simulator: 16 упакованных изображений виджетов, по 32 текстовых блока для RU и EN, размер .app уменьшен на 456 219 байт при одинаковых настройках сборки. Это около 446 КиБ и 0,34% от исходного пакета. Ручная проверка итоговых экранов в отчёте отмечена как ожидающая.')

newpage('Motorica Start запускает Fluxara Drift')
para('Коммит 2 октября согласовал группу обмена данными group.io.fluxara.drift.inputbridge, URL-схему fluxara-drive и bundle ID io.fluxara.drift. В карточке игры закреплено имя Fluxara Drift. Старый ID каталога stk принимается только тогда, когда запись действительно указывает на bundle Fluxara.')
para('После завершения первоначальной синхронизации MS выбирает вкладку датчиков. На стороне запуска обновлены ключи сохранённого запроса и проверки установленной игры. Изменения создают согласованную основу для передачи управления между приложениями.')
commits('a6a4696df')
p=para();p.alignment=WD_ALIGN_PARAGRAPH.CENTER
for idx,(path,alt) in enumerate([
    (F+'real-game-home-attachments/E9F4B9E6-A3C2-4600-88A3-C890DB5B32BC.png','Главное меню Fluxara Drift'),
    (F+'real-race-signals-attachments/37423731-DFD7-4372-A968-D00D47568E64.png','Гараж Fluxara Ace'),
    (F+'real-game-canyon-attachments/9658952A-B7E7-44DB-BCE7-0A287F2302D7.png','Настройка гонки Fluxara Canyon'),
]):
    shape=p.add_run().add_picture(image_data(path),width=Inches(2.02));shape._inline.docPr.set('descr',alt)
para('Слева направо: главное меню, гараж Fluxara Ace и подготовка гонки на Fluxara Canyon. Сохранённые скриншоты iPhone от 2 октября: 15:37:36, 15:39:20 и 15:38:35 по Москве. Все три включены в коммит MS a6a4696df.','Caption')
para('Эти кадры фиксируют вид экранов во время проверки связки. Они не означают, что сами экраны созданы на этой неделе: их реализации уже присутствовали в более ранней истории Fluxara.')

newpage('Новый вид индикаторов управления')
para('В материалах недели появились дуговые индикаторы по сторонам руля. Они сохраняют голубой корпус управления и добавляют заметное заполнение рядом с ним. В комплект входят варианты макета, образцы состояний и скриншоты проверки в игре.')
picture(F+'figma-hud-before.png',6.05,'До добавления дуговых индикаторов. Сохранённый макет Figma из материалов MS.')
picture(F+'figma-left-gauge-aligned-pause.png',6.05,'Вариант с индикаторами вокруг руля. Сохранённый макет Figma из того же коммита a6a4696df.')
para('Макеты показывают визуальное изменение, а не подтверждённое поведение сигнала. Отдельные кадры игры приведены на следующей странице; названия файлов и тестов сами по себе не доказывают корректное управление по BLE.')
commits('a6a4696df')

newpage('Кадр игры и состояние истории Fluxara')
p=para();p.alignment=WD_ALIGN_PARAGRAPH.CENTER
shape=p.add_run().add_picture(image_data(F+'hud-aligned-pause-attachments/951414DC-588E-4265-8367-5AF9AED22C86.png'),width=Inches(1.85))
shape._inline.docPr.set('descr','Полный сохранённый кадр игры с чёрной областью и HUD в нижней части')
para('Полный кадр iPhone от 2 октября, 15:52:38 по Москве. Метаданные ориентации исходного PNG учтены; чёрная область сохранена. Источник: hud-aligned-pause-attachments в коммите MS a6a4696df.','Caption')
para('В сохранённом кадре видны гонка, руль и дуговые индикаторы, но значительная часть изображения чёрная. По одному PNG нельзя определить, связано ли это с захватом изображения, ориентацией или отрисовкой приложения. Поэтому кадр подтверждает наличие визуальных элементов, а не качество полного сценария гонки.')
heading('Что зафиксировано в репозитории Fluxara',2)
para('За 28 сентября — 4 октября проверены ветки main, fluxara-drift-ios и codex/motorica-kart-ios-build28. Коммитов за период нет. Последний коммит ветки fluxara-drift-ios — e9625651 от 24 сентября, «вынесены 40 трасс в докачку». Он относится к предыдущему периоду.')
p=para('История Fluxara: ','Caption');link(p,'ветка fluxara-drift-ios','https://github.com/HakerFromRussia-XD/1_game_stk/commits/fluxara-drift-ios')
para('В локальном рабочем дереве Fluxara есть незакоммиченные изменения ресурсов, трасс, сборочных скриптов и исходников. Их даты и завершённость нельзя восстановить по истории коммитов; в результаты этой недели они не включены. Материалы связки MS и игры за неделю доступны через коммит MS.')

newpage('Коммиты и источники отчёта')
para('10 уникальных коммитов MS, без повторного подсчёта общих коммитов двух веток. Даты указаны по Москве; отбор выполнен по времени коммиттера. Коммиты слияния входят в количество, но не считаются отдельными продуктовыми функциями.')
descriptions={
    '132f05eb9':'Подготовка рефакторинга V3 и тихая фоновая телеметрия',
    '56e2d8409':'Слияние integrationV3 в clean_arch',
    '68626851a':'Поправки BLE и диалога после слияния',
    '93803ef4b':'Очередь обновления V3 и журнал формата 2',
    '085baca2e':'Повторное слияние integrationV3 в clean_arch',
    'aee8e4a97':'Импорты диалога и UI-тест после слияния',
    '11792adbc':'Следующий этап рефакторинга Android и прошивки',
    '12d54abb0':'Локализованная помощь и иллюстрации виджетов',
    'a6a4696df':'Связка MS с Fluxara и материалы проверки',
    '80a0387b8':'Перенос логики V3 и репозиториев в shared',
}
rows=[]
for c in E['ms']:
    dt=datetime.fromisoformat(c['date'].replace('Z','+00:00')).astimezone(ZoneInfo('Europe/Moscow'))
    rows.append([dt.strftime('%d.%m %H:%M'),c['sha'][:9],c['author'],descriptions[c['sha'][:9]]])
t=table(['Дата','Коммит','Автор','Изменение'],rows,[1.0,1.05,.7,4.1])
for row,c in zip(t.rows[1:],E['ms']):
    cell=row.cells[1];cell.text='';link(cell.paragraphs[0],c['sha'][:9],c['url'])
heading('Источники и границы проверки',2)
p=para('MS: ');link(p,'kmm_ubi4_integrationV3','https://github.com/HakerFromRussia-XD/2_Android_bluetooth_master_stradivary/commits/kmm_ubi4_integrationV3');p.add_run(' и ');link(p,'kmm_ubi4_with_clean_arch','https://github.com/HakerFromRussia-XD/2_Android_bluetooth_master_stradivary/commits/kmm_ubi4_with_clean_arch')
para('Проверены локальная история и актуальные удалённые ветки GitHub. Два коммита clean_arch, отсутствующие в локальной копии, изучены через GitHub. Технические изменения описаны по диффам, а картинки извлечены из сохранённых Git-объектов MS.')
para('Для иллюстраций помощи использован output/instruction-localization/previous-checkout/widgets-final.png. Метрики iOS взяты из output/instruction-localization/ios-size-validation.json; статус переноса и оставшиеся проверки — из transfer-report.json. Игровые кадры и их время взяты из output/fluxara-ios-ms-control/*-attachments/manifest.json.')
para('Сборки, тесты, прошивка устройств и публикация приложений при подготовке отчёта не выполнялись. Сохранённые результаты сборок и изображения описывают свои конкретные проверки; успешный релиз, управление во всех режимах игры и ручное принятие интерфейса из них не следуют.')

footer=SEC.footer.paragraphs[0];footer.alignment=WD_ALIGN_PARAGRAPH.RIGHT
r=footer.add_run('Motorica Start и Fluxara Drift   |   ');r.font.size=Pt(8)
field=OxmlElement('w:fldSimple');field.set(qn('w:instr'),'PAGE');footer._p.append(field)
DOC.save(OUT/'MS_Fluxara_week_2026-09-28.docx')
(OUT/'image-provenance.json').write_text(json.dumps(ASSETS,ensure_ascii=False,indent=2))
print('Saved',OUT/'MS_Fluxara_week_2026-09-28.docx')
print('Embedded image occurrences',len(ASSETS))
