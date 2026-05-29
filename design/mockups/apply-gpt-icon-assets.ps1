Add-Type -AssemblyName System.Drawing

$ErrorActionPreference = "Stop"

$source = "C:\Users\svici\.codex\generated_images\019ce97a-9b6c-79c0-9c7c-1545b7f6575a\ig_0e9787cd6cc5e286016a19879a58cc8191be61d16b13fbd7a0.png"
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
$mockupOut = Join-Path $PSScriptRoot "gpt-shift-alarm-icon.png"
$assetOut = Join-Path $repoRoot "app\src\main\res\drawable-nodpi\ic_launcher_gpt_shift.png"
$previewOut = Join-Path $PSScriptRoot "gpt-icon-applied-preview.png"

function ColorFromHex([string]$hex, [int]$alpha = 255) {
    $value = $hex.TrimStart("#")
    return [System.Drawing.Color]::FromArgb(
        $alpha,
        [Convert]::ToInt32($value.Substring(0, 2), 16),
        [Convert]::ToInt32($value.Substring(2, 2), 16),
        [Convert]::ToInt32($value.Substring(4, 2), 16)
    )
}

function IsBorderWhite([System.Drawing.Color]$c) {
    return $c.R -ge 210 -and $c.G -ge 210 -and $c.B -ge 210
}

function ClearBorderWhite([System.Drawing.Bitmap]$bitmap) {
    [int]$bw = $bitmap.Width
    [int]$bh = $bitmap.Height
    $seen = New-Object 'bool[,]' $bw, $bh
    $queue = [System.Collections.Generic.Queue[object]]::new()
    for ($x = 0; $x -lt $bw; $x++) {
        $queue.Enqueue(@($x, 0))
        $queue.Enqueue(@($x, ($bh - 1)))
    }
    for ($y = 0; $y -lt $bh; $y++) {
        $queue.Enqueue(@(0, $y))
        $queue.Enqueue(@(($bw - 1), $y))
    }

    while ($queue.Count -gt 0) {
        $p = $queue.Dequeue()
        $x = [int]$p[0]
        $y = [int]$p[1]
        if ($x -lt 0 -or $x -ge $bw -or $y -lt 0 -or $y -ge $bh) { continue }
        if ($seen[$x, $y]) { continue }
        $seen[$x, $y] = $true
        $c = $bitmap.GetPixel($x, $y)
        if ($c.A -eq 0) {
            $queue.Enqueue(@(($x + 1), $y))
            $queue.Enqueue(@(($x - 1), $y))
            $queue.Enqueue(@($x, ($y + 1)))
            $queue.Enqueue(@($x, ($y - 1)))
            continue
        }
        if (-not (IsBorderWhite $c)) { continue }
        $bitmap.SetPixel($x, $y, [System.Drawing.Color]::FromArgb(0, $c.R, $c.G, $c.B))
        $queue.Enqueue(@(($x + 1), $y))
        $queue.Enqueue(@(($x - 1), $y))
        $queue.Enqueue(@($x, ($y + 1)))
        $queue.Enqueue(@($x, ($y - 1)))
    }
}

function RoundedPath([float]$x, [float]$y, [float]$w, [float]$h, [float]$r) {
    $path = [System.Drawing.Drawing2D.GraphicsPath]::new()
    $d = $r * 2
    $path.AddArc($x, $y, $d, $d, 180, 90)
    $path.AddArc($x + $w - $d, $y, $d, $d, 270, 90)
    $path.AddArc($x + $w - $d, $y + $h - $d, $d, $d, 0, 90)
    $path.AddArc($x, $y + $h - $d, $d, $d, 90, 90)
    $path.CloseFigure()
    return $path
}

function DrawCenteredText($g, [string]$text, [float]$x, [float]$y, [float]$w, [float]$h, [float]$size, [string]$color, [System.Drawing.FontStyle]$style = [System.Drawing.FontStyle]::Regular) {
    if ($size -le 0) {
        $size = 18
    }
    $font = [System.Drawing.Font]::new("Malgun Gothic", $size, $style, [System.Drawing.GraphicsUnit]::Pixel)
    $brush = [System.Drawing.SolidBrush]::new((ColorFromHex $color))
    $format = [System.Drawing.StringFormat]::new()
    $format.Alignment = [System.Drawing.StringAlignment]::Center
    $format.LineAlignment = [System.Drawing.StringAlignment]::Center
    $g.DrawString($text, $font, $brush, [System.Drawing.RectangleF]::new($x, $y, $w, $h), $format)
    $format.Dispose()
    $brush.Dispose()
    $font.Dispose()
}

$original = [System.Drawing.Bitmap]::new($source)
[int]$w = $original.Width
[int]$h = $original.Height
$visited = New-Object 'bool[,]' $w, $h
$queue = [System.Collections.Generic.Queue[object]]::new()

for ($x = 0; $x -lt $w; $x++) {
    $queue.Enqueue(@($x, 0))
    $queue.Enqueue(@($x, ($h - 1)))
}
for ($y = 0; $y -lt $h; $y++) {
    $queue.Enqueue(@(0, $y))
    $queue.Enqueue(@(($w - 1), $y))
}

$transparent = New-Object 'bool[,]' $w, $h
while ($queue.Count -gt 0) {
    $p = $queue.Dequeue()
    $x = [int]$p[0]
    $y = [int]$p[1]
    if ($x -lt 0 -or $x -ge $w -or $y -lt 0 -or $y -ge $h) { continue }
    if ($visited[$x, $y]) { continue }
    $visited[$x, $y] = $true
    $c = $original.GetPixel($x, $y)
    if (-not (IsBorderWhite $c)) { continue }
    $transparent[$x, $y] = $true
    $queue.Enqueue(@(($x + 1), $y))
    $queue.Enqueue(@(($x - 1), $y))
    $queue.Enqueue(@($x, ($y + 1)))
    $queue.Enqueue(@($x, ($y - 1)))
}

$minX = $w
$minY = $h
$maxX = 0
$maxY = 0
for ($y = 0; $y -lt $h; $y++) {
    for ($x = 0; $x -lt $w; $x++) {
        if (-not $transparent[$x, $y]) {
            if ($x -lt $minX) { $minX = $x }
            if ($x -gt $maxX) { $maxX = $x }
            if ($y -lt $minY) { $minY = $y }
            if ($y -gt $maxY) { $maxY = $y }
        }
    }
}

$pad = 20
$minX = [Math]::Max(0, $minX - $pad)
$minY = [Math]::Max(0, $minY - $pad)
$maxX = [Math]::Min($w - 1, $maxX + $pad)
$maxY = [Math]::Min($h - 1, $maxY + $pad)
$cropW = $maxX - $minX + 1
$cropH = $maxY - $minY + 1
$cropSize = [Math]::Max($cropW, $cropH)
$cropX = [Math]::Max(0, [int]($minX - (($cropSize - $cropW) / 2)))
$cropY = [Math]::Max(0, [int]($minY - (($cropSize - $cropH) / 2)))
if ($cropX + $cropSize -gt $w) { $cropX = $w - $cropSize }
if ($cropY + $cropSize -gt $h) { $cropY = $h - $cropSize }

$assetSize = 1024
$asset = [System.Drawing.Bitmap]::new($assetSize, $assetSize, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$g = [System.Drawing.Graphics]::FromImage($asset)
$g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
$g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
$g.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighQuality
$g.Clear([System.Drawing.Color]::Transparent)
$srcRect = [System.Drawing.Rectangle]::new($cropX, $cropY, $cropSize, $cropSize)
$dstRect = [System.Drawing.Rectangle]::new(0, 0, $assetSize, $assetSize)
$iconMask = RoundedPath 0 0 $assetSize $assetSize 250
$g.SetClip($iconMask)
$g.DrawImage($original, $dstRect, $srcRect, [System.Drawing.GraphicsUnit]::Pixel)
$g.ResetClip()
$iconMask.Dispose()
$g.Dispose()
ClearBorderWhite $asset

$asset.Save($assetOut, [System.Drawing.Imaging.ImageFormat]::Png)
$asset.Save($mockupOut, [System.Drawing.Imaging.ImageFormat]::Png)

Write-Output $assetOut
Write-Output $mockupOut
return

$preview = [System.Drawing.Bitmap]::new(1500, 920)
$pg = [System.Drawing.Graphics]::FromImage($preview)
$pg.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
$pg.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
$pg.Clear((ColorFromHex "#FFF3DE"))

DrawCenteredText $pg "GPT Icon Applied" 0 58 1500 70 48 "#081A2F" ([System.Drawing.FontStyle]::Bold)
DrawCenteredText $pg "Launcher + splash preview, matched to the current cream/navy app direction" 0 126 1500 42 23 "#4D5A6C"

$phoneBg = [System.Drawing.SolidBrush]::new((ColorFromHex "#EAF5F2"))
$phonePath = RoundedPath 950 230 330 560 44
$pg.FillPath($phoneBg, $phonePath)
$phonePath.Dispose()
$phoneBg.Dispose()

$screen = [System.Drawing.Drawing2D.LinearGradientBrush]::new(
    [System.Drawing.RectangleF]::new(980, 260, 270, 500),
    (ColorFromHex "#FFF7E8"),
    (ColorFromHex "#EAF5F2"),
    [System.Drawing.Drawing2D.LinearGradientMode]::Vertical
)
$screenPath = RoundedPath 980 260 270 500 34
$pg.FillPath($screen, $screenPath)
$screenPath.Dispose()
$screen.Dispose()

$card = [System.Drawing.SolidBrush]::new((ColorFromHex "#FFFFFF"))
$cardPath = RoundedPath 1018 366 194 194 42
$pg.FillPath($card, $cardPath)
$cardPath.Dispose()
$card.Dispose()
$pg.DrawImage($asset, [System.Drawing.Rectangle]::new(1018, 366, 194, 194))

DrawCenteredText $pg "교대 알람" 980 590 270 42 27 "#172033" ([System.Drawing.FontStyle]::Bold)
DrawCenteredText $pg "시작화면/홈 톤과 맞춘 아이콘" 980 634 270 34 16 "#4D5A6C"

$cream = [System.Drawing.SolidBrush]::new((ColorFromHex "#FFF7E8"))
$pg.FillEllipse($cream, 170, 240, 260, 260)
$cream.Dispose()
$pg.DrawImage($asset, [System.Drawing.Rectangle]::new(170, 240, 260, 260))
DrawCenteredText $pg "Launcher" 130 530 340 44 26 "#172033" ([System.Drawing.FontStyle]::Bold)

$smallSizes = @(132, 86, 54)
$smallXs = @(560, 720, 842)
for ($i = 0; $i -lt $smallSizes.Count; $i++) {
    $size = $smallSizes[$i]
    $pg.DrawImage($asset, [System.Drawing.Rectangle]::new($smallXs[$i], 318 + (($smallSizes[0] - $size) / 2), $size, $size))
}
DrawCenteredText $pg "Small sizes" 520 530 430 44 26 "#172033" ([System.Drawing.FontStyle]::Bold)

DrawCenteredText $pg "Saved as app/src/main/res/drawable-nodpi/ic_launcher_gpt_shift.png" 0 828 1500 38 18 "#4D5A6C"

$pg.Dispose()
$preview.Save($previewOut, [System.Drawing.Imaging.ImageFormat]::Png)
$preview.Dispose()
$asset.Dispose()
$original.Dispose()

Write-Output $assetOut
Write-Output $mockupOut
Write-Output $previewOut
