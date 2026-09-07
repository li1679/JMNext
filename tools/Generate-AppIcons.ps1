param(
    [string]$SourcePath = (Join-Path $PSScriptRoot '../app/src/main/logo-playstore.png')
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$mainDir = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../app/src/main'))
$drawableDir = Join-Path $mainDir 'res/drawable-nodpi'
[IO.Directory]::CreateDirectory($drawableDir) | Out-Null

function Resize-Logo([System.Drawing.Image]$Source, [int]$Size) {
    $bitmap = [System.Drawing.Bitmap]::new($Size, $Size)
    $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
    try {
        $graphics.Clear([System.Drawing.Color]::White)
        $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
        $graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
        $scale = $Size / [Math]::Max($Source.Width, $Source.Height)
        $width = [int][Math]::Round($Source.Width * $scale)
        $height = [int][Math]::Round($Source.Height * $scale)
        $graphics.DrawImage($Source, [int](($Size - $width) / 2), [int](($Size - $height) / 2), $width, $height)
    } finally {
        $graphics.Dispose()
    }
    return $bitmap
}

function Save-Png([System.Drawing.Bitmap]$Bitmap, [string]$Path) {
    $Bitmap.Save($Path, [System.Drawing.Imaging.ImageFormat]::Png)
}

$inputImage = [System.Drawing.Image]::FromFile([IO.Path]::GetFullPath($SourcePath))
try {
    $logo = Resize-Logo $inputImage 512
} finally {
    $inputImage.Dispose()
}

try {
    Save-Png $logo (Join-Path $mainDir 'logo-playstore.png')
    $inApp = Resize-Logo $logo 288
    try {
        Save-Png $inApp (Join-Path $drawableDir 'app_logo.png')

        # The launcher displays the middle 72dp of a 108dp adaptive layer.
        # Scale the complete source into that area so circular masks retain JM.
        $foreground = [System.Drawing.Bitmap]::new(432, 432)
        $graphics = [System.Drawing.Graphics]::FromImage($foreground)
        try {
            $graphics.Clear([System.Drawing.Color]::White)
            $graphics.DrawImageUnscaled($inApp, 72, 72)
        } finally {
            $graphics.Dispose()
        }
        try {
            Save-Png $foreground (Join-Path $drawableDir 'ic_logo_foreground.png')
        } finally {
            $foreground.Dispose()
        }

        # Android tints the alpha channel of monochrome icons. Remove the white
        # background and faint shadow, keeping the supplied letter outlines.
        $monochrome = [System.Drawing.Bitmap]::new(432, 432)
        try {
            for ($y = 0; $y -lt $inApp.Height; $y++) {
                for ($x = 0; $x -lt $inApp.Width; $x++) {
                    $pixel = $inApp.GetPixel($x, $y)
                    $luminance = (0.2126 * $pixel.R) + (0.7152 * $pixel.G) + (0.0722 * $pixel.B)
                    $alpha = [int][Math]::Clamp((200 - $luminance) * 255 / 104, 0, 255)
                    $monochrome.SetPixel($x + 72, $y + 72, [System.Drawing.Color]::FromArgb($alpha, 0, 0, 0))
                }
            }
            Save-Png $monochrome (Join-Path $drawableDir 'ic_logo_monochrome.png')
        } finally {
            $monochrome.Dispose()
        }
    } finally {
        $inApp.Dispose()
    }

    $densities = @{ mdpi = 48; hdpi = 72; xhdpi = 96; xxhdpi = 144; xxxhdpi = 192 }
    foreach ($density in $densities.GetEnumerator()) {
        $icon = Resize-Logo $logo $density.Value
        try {
            $directory = Join-Path $mainDir "res/mipmap-$($density.Key)"
            Save-Png $icon (Join-Path $directory 'logo.png')
            Save-Png $icon (Join-Path $directory 'logo_round.png')
        } finally {
            $icon.Dispose()
        }
    }
} finally {
    $logo.Dispose()
}
