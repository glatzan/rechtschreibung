# Erzeugt das App-Icon (Verlauf Indigo→Violett mit "Ä") als PNG und ICO.
# Aufruf: powershell -ExecutionPolicy Bypass -File packaging/make-icon.ps1
Add-Type -AssemblyName System.Drawing
$root = Split-Path -Parent $MyInvocation.MyCommand.Path

function New-IconBitmap([int]$size) {
    $bmp = New-Object System.Drawing.Bitmap $size, $size
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.SmoothingMode = 'AntiAlias'
    $g.TextRenderingHint = 'AntiAliasGridFit'
    $g.Clear([System.Drawing.Color]::Transparent)
    $r = [single]($size * 0.22)
    $path = New-Object System.Drawing.Drawing2D.GraphicsPath
    $s = [single]($size - 1)
    $path.AddArc(0, 0, 2 * $r, 2 * $r, 180, 90)
    $path.AddArc($s - 2 * $r, 0, 2 * $r, 2 * $r, 270, 90)
    $path.AddArc($s - 2 * $r, $s - 2 * $r, 2 * $r, 2 * $r, 0, 90)
    $path.AddArc(0, $s - 2 * $r, 2 * $r, 2 * $r, 90, 90)
    $path.CloseFigure()
    $brush = New-Object System.Drawing.Drawing2D.LinearGradientBrush(
        (New-Object System.Drawing.PointF 0, 0), (New-Object System.Drawing.PointF $size, $size),
        [System.Drawing.Color]::FromArgb(255, 99, 102, 241), [System.Drawing.Color]::FromArgb(255, 168, 85, 247))
    $g.FillPath($brush, $path)
    $font = New-Object System.Drawing.Font("Segoe UI", [single]($size * 0.5), [System.Drawing.FontStyle]::Bold, [System.Drawing.GraphicsUnit]::Pixel)
    $fmt = New-Object System.Drawing.StringFormat
    $fmt.Alignment = 'Center'; $fmt.LineAlignment = 'Center'
    $g.DrawString([string][char]0x00C4, $font, [System.Drawing.Brushes]::White, (New-Object System.Drawing.RectangleF 0, ([single]($size * 0.04)), $size, $size), $fmt)
    $g.Dispose()
    return $bmp
}

$sizes = 16, 24, 32, 48, 64, 128, 256
$pngs = foreach ($sz in $sizes) {
    $ms = New-Object System.IO.MemoryStream
    (New-IconBitmap $sz).Save($ms, [System.Drawing.Imaging.ImageFormat]::Png)
    , $ms.ToArray()
}

# PNG für das Fenster-Icon
$resDir = Join-Path $root "..\src\main\resources"
New-Item -ItemType Directory -Force $resDir | Out-Null
[System.IO.File]::WriteAllBytes((Join-Path $resDir "icon.png"), $pngs[$sizes.IndexOf(256)])

# ICO mit PNG-Einträgen
$out = New-Object System.IO.MemoryStream
$bw = New-Object System.IO.BinaryWriter $out
$bw.Write([uint16]0); $bw.Write([uint16]1); $bw.Write([uint16]$sizes.Count)
$offset = 6 + 16 * $sizes.Count
for ($i = 0; $i -lt $sizes.Count; $i++) {
    $sz = $sizes[$i]; $len = $pngs[$i].Length
    $bw.Write([byte]($(if ($sz -ge 256) { 0 } else { $sz })))
    $bw.Write([byte]($(if ($sz -ge 256) { 0 } else { $sz })))
    $bw.Write([byte]0); $bw.Write([byte]0)
    $bw.Write([uint16]1); $bw.Write([uint16]32)
    $bw.Write([uint32]$len); $bw.Write([uint32]$offset)
    $offset += $len
}
foreach ($p in $pngs) { $bw.Write($p) }
$bw.Flush()
[System.IO.File]::WriteAllBytes((Join-Path $root "icon.ico"), $out.ToArray())
Write-Output "icon.ico und icon.png erzeugt"
