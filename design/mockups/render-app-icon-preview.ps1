Add-Type -AssemblyName System.Drawing

$ErrorActionPreference = "Stop"
$outPath = Join-Path $PSScriptRoot "app-icon-redesign-preview.png"

function ColorFromHex([string]$hex, [int]$alpha = 255) {
    $value = $hex.TrimStart("#")
    return [System.Drawing.Color]::FromArgb(
        $alpha,
        [Convert]::ToInt32($value.Substring(0, 2), 16),
        [Convert]::ToInt32($value.Substring(2, 2), 16),
        [Convert]::ToInt32($value.Substring(4, 2), 16)
    )
}

function FontOf([float]$size, [System.Drawing.FontStyle]$style = [System.Drawing.FontStyle]::Regular) {
    return [System.Drawing.Font]::new("Malgun Gothic", $size, $style, [System.Drawing.GraphicsUnit]::Pixel)
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

function FillRound($g, [float]$x, [float]$y, [float]$w, [float]$h, [float]$r, $brush) {
    $path = RoundedPath $x $y $w $h $r
    $g.FillPath($brush, $path)
    $path.Dispose()
}

function DrawText($g, [string]$text, [float]$x, [float]$y, [float]$w, [float]$h, [float]$size, [string]$color, [System.Drawing.FontStyle]$style = [System.Drawing.FontStyle]::Regular) {
    $font = FontOf $size $style
    $brush = [System.Drawing.SolidBrush]::new((ColorFromHex $color))
    $format = [System.Drawing.StringFormat]::new()
    $format.Alignment = [System.Drawing.StringAlignment]::Center
    $format.LineAlignment = [System.Drawing.StringAlignment]::Center
    $g.DrawString($text, $font, $brush, [System.Drawing.RectangleF]::new($x, $y, $w, $h), $format)
    $format.Dispose()
    $brush.Dispose()
    $font.Dispose()
}

function DrawOldIcon($g, [float]$x, [float]$y, [float]$s) {
    $bg = [System.Drawing.SolidBrush]::new((ColorFromHex "#FFF3DE"))
    FillRound $g $x $y $s $s ($s * 0.23) $bg
    $bg.Dispose()
    $card = [System.Drawing.SolidBrush]::new((ColorFromHex "#113C5F"))
    FillRound $g ($x + $s * 0.17) ($y + $s * 0.19) ($s * 0.66) ($s * 0.58) ($s * 0.14) $card
    $card.Dispose()
    $right = [System.Drawing.SolidBrush]::new((ColorFromHex "#176B87"))
    FillRound $g ($x + $s * 0.50) ($y + $s * 0.19) ($s * 0.33) ($s * 0.58) ($s * 0.14) $right
    $right.Dispose()
    $cream = [System.Drawing.SolidBrush]::new((ColorFromHex "#FFF7E8"))
    $g.FillEllipse($cream, $x + $s * 0.31, $y + $s * 0.34, $s * 0.38, $s * 0.38)
    $cream.Dispose()
    $navy = [System.Drawing.SolidBrush]::new((ColorFromHex "#113C5F"))
    $g.FillEllipse($navy, $x + $s * 0.39, $y + $s * 0.42, $s * 0.22, $s * 0.22)
    $navy.Dispose()
    $sun = [System.Drawing.SolidBrush]::new((ColorFromHex "#FFB84D"))
    $g.FillEllipse($sun, $x + $s * 0.64, $y + $s * 0.26, $s * 0.12, $s * 0.12)
    $sun.Dispose()
}

function DrawNewIcon($g, [float]$x, [float]$y, [float]$s) {
    $bgRect = [System.Drawing.RectangleF]::new($x, $y, $s, $s)
    $bg = [System.Drawing.Drawing2D.LinearGradientBrush]::new(
        $bgRect,
        (ColorFromHex "#0B314D"),
        (ColorFromHex "#18A096"),
        [System.Drawing.Drawing2D.LinearGradientMode]::ForwardDiagonal
    )
    FillRound $g $x $y $s $s ($s * 0.24) $bg
    $bg.Dispose()

    $shadow = [System.Drawing.SolidBrush]::new((ColorFromHex "#000000" 42))
    $g.FillEllipse($shadow, $x + $s * 0.22, $y + $s * 0.25, $s * 0.62, $s * 0.62)
    $shadow.Dispose()
    $cream = [System.Drawing.SolidBrush]::new((ColorFromHex "#FFF7E8"))
    $g.FillEllipse($cream, $x + $s * 0.18, $y + $s * 0.18, $s * 0.65, $s * 0.65)
    $cream.Dispose()
    $navy = [System.Drawing.SolidBrush]::new((ColorFromHex "#0E3858"))
    $g.FillEllipse($navy, $x + $s * 0.25, $y + $s * 0.25, $s * 0.50, $s * 0.50)
    $navy.Dispose()
    $teal = [System.Drawing.SolidBrush]::new((ColorFromHex "#149B8F" 188))
    $g.FillPie($teal, $x + $s * 0.25, $y + $s * 0.25, $s * 0.50, $s * 0.50, -90, 180)
    $teal.Dispose()

    $moon = [System.Drawing.SolidBrush]::new((ColorFromHex "#FFF7E8"))
    $g.FillEllipse($moon, $x + $s * 0.30, $y + $s * 0.38, $s * 0.18, $s * 0.18)
    $moon.Dispose()
    $moonCut = [System.Drawing.SolidBrush]::new((ColorFromHex "#0E3858"))
    $g.FillEllipse($moonCut, $x + $s * 0.34, $y + $s * 0.35, $s * 0.18, $s * 0.18)
    $moonCut.Dispose()

    $handPen = [System.Drawing.Pen]::new((ColorFromHex "#FFF7E8"), [Math]::Max(3, $s * 0.055))
    $handPen.StartCap = [System.Drawing.Drawing2D.LineCap]::Round
    $handPen.EndCap = [System.Drawing.Drawing2D.LineCap]::Round
    $cx = $x + $s * 0.50
    $cy = $y + $s * 0.50
    $g.DrawLine($handPen, $cx, $cy, $cx, $y + $s * 0.37)
    $g.DrawLine($handPen, $cx, $cy, $x + $s * 0.61, $y + $s * 0.56)
    $handPen.Dispose()

    $sun = [System.Drawing.SolidBrush]::new((ColorFromHex "#FFB84D"))
    $g.FillEllipse($sun, $x + $s * 0.63, $y + $s * 0.20, $s * 0.18, $s * 0.18)
    $sun.Dispose()
}

$bitmap = [System.Drawing.Bitmap]::new(1200, 780)
$g = [System.Drawing.Graphics]::FromImage($bitmap)
$g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
$g.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::AntiAliasGridFit
$g.Clear((ColorFromHex "#F6F2E8"))

DrawText $g "Cleaner Day/Night Icon" 80 46 1040 70 44 "#081A2F" ([System.Drawing.FontStyle]::Bold)
DrawText $g "Clock + crescent + sun, no extra decoration" 80 112 1040 44 22 "#4D5A6C"

DrawOldIcon $g 170 220 220
DrawNewIcon $g 520 220 220
DrawNewIcon $g 830 240 148
DrawNewIcon $g 1010 274 80
DrawText $g "Before" 170 468 220 44 24 "#667284" ([System.Drawing.FontStyle]::Bold)
DrawText $g "After" 520 468 220 44 24 "#0E3858" ([System.Drawing.FontStyle]::Bold)
DrawText $g "Small sizes" 800 468 300 44 24 "#0E3858" ([System.Drawing.FontStyle]::Bold)

DrawText $g "Why this is cleaner" 170 570 860 38 26 "#081A2F" ([System.Drawing.FontStyle]::Bold)
DrawText $g "Fewer elements    |    Strong silhouette    |    Day/night shift identity" 170 622 860 36 20 "#4D5A6C"

$g.Dispose()
$bitmap.Save($outPath, [System.Drawing.Imaging.ImageFormat]::Png)
$bitmap.Dispose()
Write-Output $outPath
