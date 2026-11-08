# Generates original StartMusicV3 PNG/ICO marks (violet disc + play). Not derived from YMusic assets.
Add-Type -AssemblyName System.Drawing

function New-BrandBitmap([int]$size) {
    $bmp = New-Object System.Drawing.Bitmap $size, $size
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $g.Clear([System.Drawing.Color]::FromArgb(0, 0, 0, 0))
    $cx = $size / 2.0
    $cy = $size / 2.0
    function Disk([double]$r, [string]$hex) {
        $c = [System.Drawing.ColorTranslator]::FromHtml($hex)
        $brush = New-Object System.Drawing.SolidBrush $c
        $d = [float](2 * $r)
        $g.FillEllipse($brush, [float]($cx - $r), [float]($cy - $r), $d, $d)
        $brush.Dispose()
    }
    Disk ($size * 0.5) '#4936A8'
    Disk ($size * 0.41) '#7657FF'
    Disk ($size * 0.29) '#8B74FF'
    $pen = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::White)
    $pts = @(
        (New-Object System.Drawing.PointF ([float]($cx - $size * 0.10), [float]($cy - $size * 0.19))),
        (New-Object System.Drawing.PointF ([float]($cx - $size * 0.10), [float]($cy + $size * 0.19))),
        (New-Object System.Drawing.PointF ([float]($cx + $size * 0.22), [float]$cy))
    )
    $g.FillPolygon($pen, $pts)
    $pen.Dispose()
    $g.Dispose()
    return $bmp
}

function Save-Ico([string]$path, [int[]]$sizes) {
    $images = New-Object System.Collections.Generic.List[byte[]]
    foreach ($s in $sizes) {
        $bmp = New-BrandBitmap $s
        $png = New-Object System.IO.MemoryStream
        $bmp.Save($png, [System.Drawing.Imaging.ImageFormat]::Png)
        $bmp.Dispose()
        [void]$images.Add($png.ToArray())
        $png.Dispose()
    }
    $ms = New-Object System.IO.MemoryStream
    $bw = New-Object System.IO.BinaryWriter $ms
    $bw.Write([uint16]0)
    $bw.Write([uint16]1)
    $bw.Write([uint16]$sizes.Count)
    $offset = 6 + (16 * $sizes.Count)
    for ($i = 0; $i -lt $sizes.Count; $i++) {
        $s = $sizes[$i]
        $data = $images[$i]
        $dim = if ($s -ge 256) { [byte]0 } else { [byte]$s }
        $bw.Write($dim)
        $bw.Write($dim)
        $bw.Write([byte]0)
        $bw.Write([byte]0)
        $bw.Write([uint16]1)
        $bw.Write([uint16]32)
        $bw.Write([uint32]$data.Length)
        $bw.Write([uint32]$offset)
        $offset += $data.Length
    }
    foreach ($data in $images) { $bw.Write($data) }
    $bw.Flush()
    [IO.File]::WriteAllBytes($path, $ms.ToArray())
    $bw.Dispose()
    $ms.Dispose()
}

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$png = New-BrandBitmap 256
$png.Save((Join-Path $root 'startmusic.png'), [System.Drawing.Imaging.ImageFormat]::Png)
$png.Dispose()
Save-Ico (Join-Path $root 'startmusic.ico') @(16, 24, 32, 48, 64, 128, 256)
# Packaging still references legacy filenames; keep them as copies of the new mark.
Copy-Item (Join-Path $root 'startmusic.ico') (Join-Path $root 'ymusic.ico') -Force
Copy-Item (Join-Path $root 'startmusic.png') (Join-Path $root 'ymusic.png') -Force
Write-Host 'Wrote startmusic.png / startmusic.ico (+ ymusic.* copies for existing packaging paths)'
