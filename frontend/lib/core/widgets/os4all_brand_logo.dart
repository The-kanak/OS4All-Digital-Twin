import 'package:flutter/material.dart';
import '../theme/app_theme.dart';

class Os4AllBrandLogo extends StatelessWidget {
  final double fontSize;
  final Color textColor;
  final Color osFillColor;
  final Color osBorderColor;
  final Color accentColor;
  final bool showTwinBadge;

  const Os4AllBrandLogo({
    super.key,
    this.fontSize = 20,
    this.textColor = AppTheme.textPrimary,
    this.osFillColor = Colors.white,
    this.osBorderColor = AppTheme.textPrimary,
    this.accentColor = AppTheme.primary,
    this.showTwinBadge = false,
  });

  @override
  Widget build(BuildContext context) {
    final strokeWidth = (fontSize * 0.08).clamp(1.3, 2.2);
    final underlineHeight = (fontSize * 0.11).clamp(2.0, 3.2);

    return Semantics(
      label: 'OS4All',
      child: Row(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.center,
        children: [
          // 'OS' text with signature white fill, dark border, and blue underline
          Stack(
            clipBehavior: Clip.none,
            alignment: Alignment.centerLeft,
            children: [
              // 1. Thin border stroke
              Text(
                'OS',
                style: TextStyle(
                  fontSize: fontSize,
                  fontWeight: FontWeight.w900,
                  letterSpacing: -0.5,
                  height: 1.1,
                  foreground: Paint()
                    ..style = PaintingStyle.stroke
                    ..strokeWidth = strokeWidth
                    ..strokeJoin = StrokeJoin.round
                    ..color = osBorderColor,
                ),
              ),
              // 2. White fill
              Text(
                'OS',
                style: TextStyle(
                  fontSize: fontSize,
                  fontWeight: FontWeight.w900,
                  letterSpacing: -0.5,
                  height: 1.1,
                  color: osFillColor,
                ),
              ),
              // 3. Signature royal blue underline bar
              Positioned(
                bottom: -underlineHeight - 1.5,
                left: 0,
                right: 0,
                child: Container(
                  height: underlineHeight,
                  decoration: BoxDecoration(
                    color: accentColor,
                    borderRadius: BorderRadius.circular(1.5),
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(width: 4),
          // '4All' text
          Text(
            '4All',
            style: TextStyle(
              color: textColor,
              fontWeight: FontWeight.w900,
              fontSize: fontSize,
              letterSpacing: -0.5,
              height: 1.1,
            ),
          ),
          if (showTwinBadge) ...[
            const SizedBox(width: 8),
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
              decoration: BoxDecoration(
                color: AppTheme.primary.withOpacity(0.15),
                borderRadius: BorderRadius.circular(4),
                border: Border.all(color: AppTheme.primary.withOpacity(0.4), width: 1),
              ),
              child: Text(
                'DIGITAL TWIN',
                style: TextStyle(
                  color: AppTheme.primary,
                  fontWeight: FontWeight.w800,
                  fontSize: (fontSize * 0.45).clamp(8.0, 11.0),
                  letterSpacing: 0.8,
                ),
              ),
            ),
          ],
        ],
      ),
    );
  }
}
