#!/usr/bin/env python3
"""
Скрипт для генерации Android иконок из исходного PNG файла.
Создает все необходимые размеры для разных плотностей экрана.
"""

import os
from PIL import Image, ImageDraw
import sys

# Размеры для адаптивных иконок (108dp)
ADAPTIVE_ICON_SIZES = {
    'mdpi': 108,    # 1x
    'hdpi': 162,    # 1.5x
    'xhdpi': 216,   # 2x
    'xxhdpi': 324,  # 3x
    'xxxhdpi': 432  # 4x
}

# Размеры для обычных иконок (48dp) - для обратной совместимости
LEGACY_ICON_SIZES = {
    'mdpi': 48,
    'hdpi': 72,
    'xhdpi': 96,
    'xxhdpi': 144,
    'xxxhdpi': 192
}

def create_adaptive_icon(source_image_path, output_dir, density, size):
    """Создает адаптивную иконку с безопасной зоной 72dp."""
    # Загружаем исходное изображение
    source = Image.open(source_image_path).convert('RGBA')
    
    # Размер canvas для адаптивной иконки
    canvas_size = size
    # Безопасная зона (72dp) - это 2/3 от canvas (108dp)
    safe_zone = int(size * 72 / 108)
    
    # Создаем canvas с черным фоном
    canvas = Image.new('RGBA', (canvas_size, canvas_size), (0, 0, 0, 255))
    
    # Масштабируем исходное изображение до безопасной зоны
    # Сохраняем пропорции
    source.thumbnail((safe_zone, safe_zone), Image.Resampling.LANCZOS)
    
    # Центрируем изображение на canvas
    x_offset = (canvas_size - source.width) // 2
    y_offset = (canvas_size - source.height) // 2
    
    # Вставляем изображение на canvas
    canvas.paste(source, (x_offset, y_offset), source)
    
    # Сохраняем
    output_path = os.path.join(output_dir, f'ic_launcher_foreground.png')
    canvas.save(output_path, 'PNG', optimize=True)
    print(f"Created: {output_path} ({canvas_size}x{canvas_size})")
    
    return canvas

def create_legacy_icon(source_image_path, output_dir, density, size):
    """Создает обычную иконку для обратной совместимости."""
    # Загружаем исходное изображение
    source = Image.open(source_image_path).convert('RGBA')
    
    # Создаем canvas с прозрачным фоном
    canvas = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    
    # Масштабируем исходное изображение до размера иконки
    # Сохраняем пропорции
    source.thumbnail((size, size), Image.Resampling.LANCZOS)
    
    # Центрируем изображение
    x_offset = (size - source.width) // 2
    y_offset = (size - source.height) // 2
    
    # Вставляем изображение на canvas
    canvas.paste(source, (x_offset, y_offset), source)
    
    # Сохраняем
    output_path = os.path.join(output_dir, f'ic_launcher.png')
    canvas.save(output_path, 'PNG', optimize=True)
    print(f"Created: {output_path} ({size}x{size})")
    
    return canvas

def create_round_icon(source_image_path, output_dir, density, size):
    """Создает круглую иконку для обратной совместимости."""
    # Загружаем исходное изображение
    source = Image.open(source_image_path).convert('RGBA')
    
    # Создаем canvas с прозрачным фоном
    canvas = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    
    # Создаем маску для круглой формы
    mask = Image.new('L', (size, size), 0)
    draw = ImageDraw.Draw(mask)
    draw.ellipse((0, 0, size, size), fill=255)
    
    # Масштабируем исходное изображение до размера иконки
    # Сохраняем пропорции
    source.thumbnail((size, size), Image.Resampling.LANCZOS)
    
    # Центрируем изображение
    x_offset = (size - source.width) // 2
    y_offset = (size - source.height) // 2
    
    # Создаем временный canvas для изображения
    temp_canvas = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    temp_canvas.paste(source, (x_offset, y_offset), source)
    
    # Применяем круглую маску
    canvas = Image.composite(temp_canvas, canvas, mask)
    
    # Сохраняем
    output_path = os.path.join(output_dir, f'ic_launcher_round.png')
    canvas.save(output_path, 'PNG', optimize=True)
    print(f"Created: {output_path} ({size}x{size})")
    
    return canvas

def create_bitmap_drawable_xml(output_dir, density):
    """Создает XML файл для bitmap drawable."""
    xml_content = f'''<?xml version="1.0" encoding="utf-8"?>
<bitmap xmlns:android="http://schemas.android.com/apk/res/android"
    android:src="@mipmap/ic_launcher_foreground" />
'''
    output_path = os.path.join(output_dir, 'ic_launcher_foreground.xml')
    with open(output_path, 'w') as f:
        f.write(xml_content)
    print(f"Created: {output_path}")

def main():
    # Пути
    project_root = os.path.dirname(os.path.abspath(__file__))
    source_image = os.path.join(project_root, 'app', 'src', 'main', 'assets', 'ic_launcher.png')
    res_dir = os.path.join(project_root, 'app', 'src', 'main', 'res')
    
    if not os.path.exists(source_image):
        print(f"Error: Source image not found at {source_image}")
        sys.exit(1)
    
    # Создаем иконки для всех плотностей
    for density, adaptive_size in ADAPTIVE_ICON_SIZES.items():
        mipmap_dir = os.path.join(res_dir, f'mipmap-{density}')
        os.makedirs(mipmap_dir, exist_ok=True)
        
        # Создаем адаптивную иконку (foreground) в mipmap
        create_adaptive_icon(source_image, mipmap_dir, density, adaptive_size)
        
        # Создаем обычную иконку для обратной совместимости
        legacy_size = LEGACY_ICON_SIZES[density]
        create_legacy_icon(source_image, mipmap_dir, density, legacy_size)
        
        # Создаем круглую иконку для обратной совместимости
        create_round_icon(source_image, mipmap_dir, density, legacy_size)
    
    # Создаем bitmap drawable XML для foreground (использует mipmap ресурсы)
    drawable_dir = os.path.join(res_dir, 'drawable')
    os.makedirs(drawable_dir, exist_ok=True)
    create_bitmap_drawable_xml(drawable_dir, 'default')
    
    print("\n✓ All icons generated successfully!")

if __name__ == '__main__':
    main()

