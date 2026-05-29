Add-Type -AssemblyName System.Drawing

$ErrorActionPreference = "Stop"

$scriptRoot = if ([string]::IsNullOrWhiteSpace($PSScriptRoot)) {
    Join-Path (Get-Location).Path "design\mockups"
} else {
    $PSScriptRoot
}
$outPath = Join-Path $scriptRoot "shift-calm-concept-board.png"
$width = 1800
$height = 1560

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
    $path = New-Object System.Drawing.Drawing2D.GraphicsPath
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

function StrokeRound($g, [float]$x, [float]$y, [float]$w, [float]$h, [float]$r, [string]$hex, [float]$stroke = 1.5, [int]$alpha = 255) {
    $path = RoundedPath $x $y $w $h $r
    $pen = New-Object System.Drawing.Pen((ColorFromHex $hex $alpha), $stroke)
    $g.DrawPath($pen, $path)
    $pen.Dispose()
    $path.Dispose()
}

function DrawText($g, [string]$text, [float]$x, [float]$y, [float]$w, [float]$h, [float]$size, [string]$color, [System.Drawing.FontStyle]$style = [System.Drawing.FontStyle]::Regular, [string]$align = "Near") {
    $font = FontOf $size $style
    $brush = New-Object System.Drawing.SolidBrush((ColorFromHex $color))
    $format = New-Object System.Drawing.StringFormat
    $format.Alignment = [System.Drawing.StringAlignment]::$align
    $format.LineAlignment = [System.Drawing.StringAlignment]::Near
    $format.Trimming = [System.Drawing.StringTrimming]::EllipsisCharacter
    $rect = [System.Drawing.RectangleF]::new($x, $y, $w, $h)
    $g.DrawString($text, $font, $brush, $rect, $format)
    $format.Dispose()
    $brush.Dispose()
    $font.Dispose()
}

function DrawCenteredText($g, [string]$text, [float]$x, [float]$y, [float]$w, [float]$h, [float]$size, [string]$color, [System.Drawing.FontStyle]$style = [System.Drawing.FontStyle]::Regular) {
    $font = FontOf $size $style
    $brush = New-Object System.Drawing.SolidBrush((ColorFromHex $color))
    $format = New-Object System.Drawing.StringFormat
    $format.Alignment = [System.Drawing.StringAlignment]::Center
    $format.LineAlignment = [System.Drawing.StringAlignment]::Center
    $rect = [System.Drawing.RectangleF]::new($x, $y, $w, $h)
    $g.DrawString($text, $font, $brush, $rect, $format)
    $format.Dispose()
    $brush.Dispose()
    $font.Dispose()
}

function DrawPill($g, [string]$text, [float]$x, [float]$y, [float]$w, [float]$h, [string]$bg, [string]$fg, [float]$size = 18, [bool]$bold = $true) {
    $brush = New-Object System.Drawing.SolidBrush((ColorFromHex $bg))
    FillRound $g $x $y $w $h ($h / 2) $brush
    $brush.Dispose()
    $style = if ($bold) { [System.Drawing.FontStyle]::Bold } else { [System.Drawing.FontStyle]::Regular }
    DrawCenteredText $g $text $x $y $w $h $size $fg $style
}

function DrawCard($g, [float]$x, [float]$y, [float]$w, [float]$h, [float]$r = 28, [string]$fill = "#FFFFFF", [string]$stroke = "#D9E2DE") {
    $brush = New-Object System.Drawing.SolidBrush((ColorFromHex $fill))
    FillRound $g $x $y $w $h $r $brush
    $brush.Dispose()
    StrokeRound $g $x $y $w $h $r $stroke 1.6 255
}

function DrawPhoneShell($g, [float]$x, [float]$y, [float]$w, [float]$h, [string]$label) {
    $shadow = New-Object System.Drawing.SolidBrush((ColorFromHex "#093C5C" 20))
    FillRound $g ($x + 16) ($y + 22) $w $h 46 $shadow
    $shadow.Dispose()
    $shell = New-Object System.Drawing.SolidBrush((ColorFromHex "#FFFFFF"))
    FillRound $g $x $y $w $h 46 $shell
    $shell.Dispose()
    StrokeRound $g $x $y $w $h 46 "#DAE2DE" 2
    $screen = [System.Drawing.Drawing2D.LinearGradientBrush]::new(
        [System.Drawing.RectangleF]::new($x + 20, $y + 20, $w - 40, $h - 40),
        (ColorFromHex "#FFF7E7"),
        (ColorFromHex "#EEF7F3"),
        [System.Drawing.Drawing2D.LinearGradientMode]::Vertical
    )
    FillRound $g ($x + 20) ($y + 20) ($w - 40) ($h - 40) 34 $screen
    $screen.Dispose()
    DrawCenteredText $g $label ($x + 20) ($y + $h + 18) ($w - 40) 34 24 "#132033" ([System.Drawing.FontStyle]::Bold)
}

function DrawBottomNav($g, [float]$x, [float]$y, [float]$w, [string]$active) {
    DrawCard $g $x $y $w 72 30 "#FFFFFF" "#DDE7E3"
    $items = @(
        @{Key="today"; Label="오늘"; Icon="⌂"},
        @{Key="pattern"; Label="패턴"; Icon="✎"},
        @{Key="manage"; Label="관리"; Icon="⚙"}
    )
    $cell = $w / 3
    for ($i = 0; $i -lt $items.Count; $i++) {
        $it = $items[$i]
        $cx = $x + ($cell * $i)
        if ($it.Key -eq $active) {
            DrawPill $g $it.Label ($cx + 24) ($y + 14) ($cell - 48) 44 "#FFE8B9" "#103B5D" 18 $true
        } else {
            DrawCenteredText $g $it.Label ($cx + 16) ($y + 18) ($cell - 32) 38 18 "#667284" ([System.Drawing.FontStyle]::Bold)
        }
    }
}

function DrawMiniCalendar($g, [float]$x, [float]$y, [float]$w) {
    $labels = @("월","화","수","목","금","토","일")
    $cell = ($w - 30) / 7
    for ($i = 0; $i -lt 7; $i++) {
        DrawCenteredText $g $labels[$i] ($x + 15 + $cell * $i) $y $cell 22 15 "#5A6678" ([System.Drawing.FontStyle]::Bold)
    }
    $days = @(
        @{D="25"; T="휴"; C="#F4F6F5"; F="#667284"},
        @{D="26"; T="주"; C="#FFECC4"; F="#9A6500"},
        @{D="27"; T="야"; C="#E7EBFF"; F="#293A78"},
        @{D="28"; T="당"; C="#DDF1EF"; F="#0F6B64"},
        @{D="29"; T="주"; C="#FFF8E9"; F="#12324D"},
        @{D="30"; T="휴"; C="#F4F6F5"; F="#667284"},
        @{D="31"; T="야"; C="#E7EBFF"; F="#293A78"}
    )
    for ($i = 0; $i -lt 7; $i++) {
        $d = $days[$i]
        $bx = $x + 15 + $cell * $i + 4
        $by = $y + 34
        $brush = New-Object System.Drawing.SolidBrush((ColorFromHex $d.C))
        FillRound $g $bx $by ($cell - 8) 64 18 $brush
        $brush.Dispose()
        if ($d.D -eq "29") {
            StrokeRound $g $bx $by ($cell - 8) 64 18 "#F3A321" 2.2
        }
        DrawCenteredText $g $d.D $bx ($by + 7) ($cell - 8) 24 14 $d.F ([System.Drawing.FontStyle]::Bold)
        DrawCenteredText $g $d.T $bx ($by + 29) ($cell - 8) 26 15 $d.F ([System.Drawing.FontStyle]::Bold)
    }
}

function DrawHomeScreen($g, [float]$x, [float]$y, [float]$w, [float]$h) {
    DrawText $g "교대 알람" ($x + 42) ($y + 38) 150 36 22 "#132033" ([System.Drawing.FontStyle]::Bold)
    DrawPill $g "신뢰도 정상" ($x + $w - 178) ($y + 36) 126 34 "#DDF1EF" "#0B7168" 15 $true

    $heroBrush = [System.Drawing.Drawing2D.LinearGradientBrush]::new(
        [System.Drawing.RectangleF]::new($x + 42, $y + 92, $w - 84, 170),
        (ColorFromHex "#103B5D"),
        (ColorFromHex "#149B8F"),
        [System.Drawing.Drawing2D.LinearGradientMode]::ForwardDiagonal
    )
    FillRound $g ($x + 42) ($y + 92) ($w - 84) 170 30 $heroBrush
    $heroBrush.Dispose()
    DrawText $g "오늘 근무" ($x + 72) ($y + 122) 160 28 18 "#D8F3EE" ([System.Drawing.FontStyle]::Bold)
    DrawText $g "주간" ($x + 72) ($y + 154) 130 60 42 "#FFFFFF" ([System.Drawing.FontStyle]::Bold)
    DrawText $g "다음 알람" ($x + 250) ($y + 128) 120 24 16 "#BCE7E0" ([System.Drawing.FontStyle]::Bold)
    DrawText $g "07:00" ($x + 250) ($y + 156) 180 64 46 "#FFFFFF" ([System.Drawing.FontStyle]::Bold)
    DrawPill $g "6시간 12분 후" ($x + 72) ($y + 215) 160 34 "#FFFFFF" "#103B5D" 15 $true
    DrawPill $g "ON" ($x + $w - 118) ($y + 126) 56 56 "#FFB84D" "#082A43" 16 $true

    DrawCard $g ($x + 42) ($y + 292) ($w - 84) 250 28 "#FFFFFF" "#D9E2DE"
    DrawText $g "2026년 5월" ($x + 70) ($y + 318) 220 38 26 "#132033" ([System.Drawing.FontStyle]::Bold)
    DrawPill $g "<" ($x + $w - 138) ($y + 316) 38 38 "#EEF4F1" "#153B5C" 18 $true
    DrawPill $g ">" ($x + $w - 92) ($y + 316) 38 38 "#EEF4F1" "#153B5C" 18 $true
    DrawMiniCalendar $g ($x + 62) ($y + 365) ($w - 124)
    DrawPill $g "선택 날짜 5/29 금" ($x + 70) ($y + 480) 160 34 "#FFF1CF" "#9A6500" 15 $true

    DrawCard $g ($x + 42) ($y + 572) ($w - 84) 128 28 "#EEF7F3" "#D9E2DE"
    DrawText $g "빠른 처리" ($x + 70) ($y + 596) 150 28 20 "#132033" ([System.Drawing.FontStyle]::Bold)
    DrawText $g "휴가/스킵/변경을 한 번에 처리" ($x + 70) ($y + 630) ($w - 140) 28 18 "#4D5A6C"
    DrawPill $g "휴가" ($x + 70) ($y + 660) 86 36 "#103B5D" "#FFFFFF" 16 $true
    DrawPill $g "스킵" ($x + 166) ($y + 660) 86 36 "#FFFFFF" "#103B5D" 16 $true
    DrawPill $g "변경" ($x + 262) ($y + 660) 86 36 "#FFFFFF" "#103B5D" 16 $true
    DrawBottomNav $g ($x + 42) ($y + $h - 112) ($w - 84) "today"
}

function DrawPatternScreen($g, [float]$x, [float]$y, [float]$w, [float]$h) {
    DrawText $g "패턴 만들기" ($x + 42) ($y + 38) 220 36 24 "#132033" ([System.Drawing.FontStyle]::Bold)
    DrawPill $g "3단계" ($x + $w - 130) ($y + 38) 78 34 "#E7EBFF" "#293A78" 15 $true

    DrawCard $g ($x + 42) ($y + 90) ($w - 84) 126 30 "#FFFFFF" "#D9E2DE"
    DrawText $g "반복 규칙" ($x + 70) ($y + 112) 180 28 20 "#132033" ([System.Drawing.FontStyle]::Bold)
    DrawText $g "주간 2일 · 야간 2일 · 휴무 2일" ($x + 70) ($y + 145) 300 30 18 "#4D5A6C"
    DrawPill $g "추천" ($x + $w - 124) ($y + 125) 72 42 "#DDF1EF" "#0B7168" 16 $true

    DrawCard $g ($x + 42) ($y + 244) ($w - 84) 310 30 "#FFFFFF" "#D9E2DE"
    DrawText $g "근무 리듬" ($x + 70) ($y + 270) 180 34 24 "#132033" ([System.Drawing.FontStyle]::Bold)
    $steps = @(
        @{T="주"; C="#FFECC4"; F="#9A6500"; L="07:00"},
        @{T="주"; C="#FFECC4"; F="#9A6500"; L="07:00"},
        @{T="야"; C="#E7EBFF"; F="#293A78"; L="18:30"},
        @{T="야"; C="#E7EBFF"; F="#293A78"; L="18:30"},
        @{T="휴"; C="#EEF4F1"; F="#667284"; L="OFF"},
        @{T="휴"; C="#EEF4F1"; F="#667284"; L="OFF"}
    )
    for ($i = 0; $i -lt $steps.Count; $i++) {
        $s = $steps[$i]
        $px = $x + 70 + (($i % 3) * 122)
        $py = $y + 322 + ([Math]::Floor($i / 3) * 94)
        $brush = New-Object System.Drawing.SolidBrush((ColorFromHex $s.C))
        FillRound $g $px $py 96 72 22 $brush
        $brush.Dispose()
        DrawCenteredText $g $s.T $px ($py + 10) 96 26 23 $s.F ([System.Drawing.FontStyle]::Bold)
        DrawCenteredText $g $s.L $px ($py + 40) 96 22 14 $s.F ([System.Drawing.FontStyle]::Bold)
    }
    DrawPill $g "미리보기" ($x + 70) ($y + 498) 104 38 "#103B5D" "#FFFFFF" 16 $true
    DrawText $g "다음 14일 알람 자동 생성" ($x + 186) ($y + 503) 270 28 16 "#4D5A6C"

    DrawCard $g ($x + 42) ($y + 582) ($w - 84) 160 30 "#FFF8EA" "#EADCC2"
    DrawText $g "실수 방지" ($x + 70) ($y + 608) 160 32 22 "#132033" ([System.Drawing.FontStyle]::Bold)
    DrawText $g "저장 전 충돌 날짜, 꺼진 알람, 권한 상태를 한 번에 확인합니다." ($x + 70) ($y + 642) ($w - 140) 52 17 "#4D5A6C"
    DrawPill $g "저장 전 점검" ($x + 70) ($y + 696) 132 38 "#FFB84D" "#082A43" 16 $true
    DrawBottomNav $g ($x + 42) ($y + $h - 112) ($w - 84) "pattern"
}

function DrawManageScreen($g, [float]$x, [float]$y, [float]$w, [float]$h) {
    DrawText $g "신뢰도 센터" ($x + 42) ($y + 38) 220 36 24 "#132033" ([System.Drawing.FontStyle]::Bold)
    DrawPill $g "주의 2건" ($x + $w - 146) ($y + 38) 94 34 "#FFF1CF" "#9A6500" 15 $true

    DrawCard $g ($x + 42) ($y + 92) ($w - 84) 168 30 "#103B5D" "#103B5D"
    DrawText $g "다음 알람 보호" ($x + 70) ($y + 120) 180 28 18 "#CDEBE5" ([System.Drawing.FontStyle]::Bold)
    DrawText $g "07:00" ($x + 70) ($y + 148) 190 72 56 "#FFFFFF" ([System.Drawing.FontStyle]::Bold)
    DrawPill $g "켜짐" ($x + $w - 132) ($y + 126) 80 50 "#FFB84D" "#082A43" 18 $true
    DrawText $g "재부팅 복구 · 워치 알림 · 빠른 테스트 준비" ($x + 70) ($y + 218) 330 28 16 "#CDEBE5"

    DrawCard $g ($x + 42) ($y + 290) ($w - 84) 212 30 "#FFFFFF" "#D9E2DE"
    DrawText $g "빠른 알람 테스트" ($x + 70) ($y + 318) 230 34 22 "#132033" ([System.Drawing.FontStyle]::Bold)
    DrawText $g "울림, 예약 상태, OS 예약 경로를 분리 확인" ($x + 70) ($y + 352) 340 44 17 "#4D5A6C"
    DrawPill $g "지금 울려보기" ($x + 70) ($y + 414) 158 46 "#103B5D" "#FFFFFF" 16 $true
    DrawPill $g "예약 점검" ($x + 242) ($y + 414) 126 46 "#EEF4F1" "#103B5D" 16 $true
    DrawText $g "권한 꺼짐 / 배터리 제한 확인 필요" ($x + 70) ($y + 468) 320 24 16 "#9A6500" ([System.Drawing.FontStyle]::Bold)

    DrawCard $g ($x + 42) ($y + 532) ($w - 84) 206 30 "#FFFFFF" "#D9E2DE"
    DrawText $g "상태 체크리스트" ($x + 70) ($y + 560) 220 32 22 "#132033" ([System.Drawing.FontStyle]::Bold)
    $checks = @(
        @{T="부팅 후 자동 복구"; S="정상"; C="#DDF1EF"; F="#0B7168"},
        @{T="정확 알람 권한"; S="확인"; C="#FFF1CF"; F="#9A6500"},
        @{T="배터리 제한"; S="확인"; C="#FFF1CF"; F="#9A6500"}
    )
    for ($i = 0; $i -lt $checks.Count; $i++) {
        $c = $checks[$i]
        $cy = $y + 608 + ($i * 42)
        DrawText $g $c.T ($x + 70) $cy 220 28 17 "#4D5A6C"
        DrawPill $g $c.S ($x + $w - 130) ($cy - 3) 78 32 $c.C $c.F 14 $true
    }
    DrawBottomNav $g ($x + 42) ($y + $h - 112) ($w - 84) "manage"
}

function DrawIconConcepts($g, [float]$x, [float]$y) {
    DrawText $g "앱 아이콘 방향" $x $y 280 36 26 "#132033" ([System.Drawing.FontStyle]::Bold)
    DrawText $g "시계 + 해/달 + 교대 리듬을 단순 심볼로 통합" $x ($y + 40) 520 30 18 "#4D5A6C"
    $icons = @(
        @{Label="추천"; Bg1="#103B5D"; Bg2="#149B8F"; Sun="#FFB84D"},
        @{Label="라이트"; Bg1="#FFF3D8"; Bg2="#DDF1EF"; Sun="#103B5D"},
        @{Label="워치"; Bg1="#172033"; Bg2="#293A78"; Sun="#FFB84D"}
    )
    for ($i = 0; $i -lt $icons.Count; $i++) {
        $ic = $icons[$i]
        $ix = $x + ($i * 154)
        $iy = $y + 92
        $grad = [System.Drawing.Drawing2D.LinearGradientBrush]::new(
            [System.Drawing.RectangleF]::new($ix, $iy, 116, 116),
            (ColorFromHex $ic.Bg1),
            (ColorFromHex $ic.Bg2),
            [System.Drawing.Drawing2D.LinearGradientMode]::ForwardDiagonal
        )
        FillRound $g $ix $iy 116 116 28 $grad
        $grad.Dispose()
        $pen = New-Object System.Drawing.Pen((ColorFromHex "#FFFFFF" 230), 8)
        $g.DrawEllipse($pen, ($ix + 28), ($iy + 28), 60, 60)
        $g.DrawLine($pen, ($ix + 58), ($iy + 58), ($ix + 58), ($iy + 38))
        $g.DrawLine($pen, ($ix + 58), ($iy + 58), ($ix + 76), ($iy + 68))
        $pen.Dispose()
        $sunBrush = New-Object System.Drawing.SolidBrush((ColorFromHex $ic.Sun))
        $g.FillEllipse($sunBrush, ($ix + 70), ($iy + 22), 30, 30)
        $sunBrush.Dispose()
        DrawCenteredText $g $ic.Label $ix ($iy + 126) 116 28 16 "#132033" ([System.Drawing.FontStyle]::Bold)
    }
}

$bitmap = [System.Drawing.Bitmap]::new($width, $height)
$graphics = [System.Drawing.Graphics]::FromImage($bitmap)
$graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
$graphics.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::AntiAliasGridFit
$graphics.Clear((ColorFromHex "#F5F1E6"))

$bgBrush = [System.Drawing.Drawing2D.LinearGradientBrush]::new(
    [System.Drawing.RectangleF]::new(0, 0, $width, $height),
    (ColorFromHex "#FFF6E7"),
    (ColorFromHex "#EAF5F2"),
    [System.Drawing.Drawing2D.LinearGradientMode]::ForwardDiagonal
)
$graphics.FillRectangle($bgBrush, 0, 0, $width, $height)
$bgBrush.Dispose()

$softBlue = New-Object System.Drawing.SolidBrush((ColorFromHex "#BFE4DF" 115))
$graphics.FillEllipse($softBlue, 1450, -70, 310, 310)
$graphics.FillEllipse($softBlue, 1360, 970, 280, 390)
$softBlue.Dispose()
$softSun = New-Object System.Drawing.SolidBrush((ColorFromHex "#FFE1A4" 140))
$graphics.FillEllipse($softSun, 60, 56, 220, 220)
$softSun.Dispose()

DrawText $graphics "교대 알람 디자인 1차 시안" 96 60 820 70 46 "#081A2F" ([System.Drawing.FontStyle]::Bold)
DrawText $graphics "목표: 매일 쓰는 알람앱처럼 빠르고, 재부팅/권한 상태는 신뢰도 센터에서 바로 보이게." 100 128 920 36 22 "#4D5A6C"
DrawPill $graphics "추천 방향: Shift Calm" 1050 78 260 46 "#103B5D" "#FFFFFF" 18 $true
DrawPill $graphics "크림 · 딥네이비 · 세이지 · 앰버" 1328 78 328 46 "#FFFFFF" "#103B5D" 18 $true

DrawPhoneShell $graphics 110 205 430 890 "홈: 오늘과 다음 알람"
DrawHomeScreen $graphics 110 205 430 890

DrawPhoneShell $graphics 684 205 430 890 "패턴: 교대 리듬 만들기"
DrawPatternScreen $graphics 684 205 430 890

DrawPhoneShell $graphics 1258 205 430 890 "관리: 신뢰도와 빠른 테스트"
DrawManageScreen $graphics 1258 205 430 890

DrawIconConcepts $graphics 110 1160

DrawText $graphics "적용하면 좋아지는 점" 684 1160 300 36 26 "#132033" ([System.Drawing.FontStyle]::Bold)
DrawPill $graphics "1. 홈은 다음 알람만 크게" 684 1210 240 42 "#FFFFFF" "#103B5D" 16 $true
DrawPill $graphics "2. 권한 문제는 체크리스트화" 940 1210 260 42 "#FFFFFF" "#103B5D" 16 $true
DrawPill $graphics "3. 아이콘/스플래시 톤 통일" 1218 1210 270 42 "#FFFFFF" "#103B5D" 16 $true
DrawText $graphics "확정하면 DesignSystem -> HomePage -> EditorPage -> 아이콘 순서로 바로 코드에 옮깁니다." 684 1266 880 44 18 "#4D5A6C"

$graphics.Dispose()
$bitmap.Save($outPath, [System.Drawing.Imaging.ImageFormat]::Png)
$bitmap.Dispose()

Write-Output $outPath
