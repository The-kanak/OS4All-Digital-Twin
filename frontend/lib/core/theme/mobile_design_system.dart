import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

/// OS4All Mobile Design System Tokens
/// Unified Dark Healthcare-Tech aesthetic optimized for mobile viewports (e.g. 1080x2400 / 392dp Android devices)
class MobileTheme {
  // Foundations
  static const Color background = Color(0xFFF6F9FC);      // Clean Light Clinical Background (#F6F9FC)
  static const Color surface = Color(0xFFFFFFFF);         // Clean White Card Surface (#FFFFFF)
  static const Color surfaceElevated = Color(0xFFF8FBFF); // Secondary Surface / Elevated (#F8FBFF)
  static const Color border = Color(0xFFE2E8F0);          // Subtle Card Border (#E2E8F0)
  static const Color borderSubtle = Color(0xFFF1F5F9);    // Very Subtle Border (#F1F5F9)

  // Status & Telemetry Accents
  static const Color primary = Color(0xFF2563EB);         // Primary Brand Blue (#2563EB)
  static const Color primaryDark = Color(0xFF1D4ED8);     // Dark Blue (#1D4ED8)
  static const Color softBlue = Color(0xFFDBEAFE);        // Soft Blue Accent (#DBEAFE)
  static const Color lightBlueAccent = Color(0xFFEFF6FF); // Very Light Blue Accent (#EFF6FF)
  static const Color secondaryAccent = Color(0xFF4F46E5); // AI Accent Indigo (#4F46E5)
  static const Color stable = Color(0xFF16A34A);          // Stable: Emerald Green (#16A34A)
  static const Color drift = Color(0xFFD97706);           // Drift: Amber Gold (#D97706)
  static const Color critical = Color(0xFFDC2626);        // Critical: Coral Red (#DC2626)
  static const Color warning = Color(0xFFD97706);         // Alias for drift
  static const Color geminiPurple = Color(0xFF4F46E5);    // Gemini AI Accent (#4F46E5)

  // Semantic Status Backgrounds
  static const Color successBg = Color(0xFFDCFCE7);       // Green 100
  static const Color warningBg = Color(0xFFFEF3C7);       // Amber 100
  static const Color criticalBg = Color(0xFFFEE2E2);      // Red 100
  static const Color aiBg = Color(0xFFEEF2FF);            // Indigo 100
  static const Color primaryBg = Color(0xFFEFF6FF);       // Blue 50

  // Semantic Status Borders
  static const Color successBorder = Color(0xFFBBF7D0);
  static const Color warningBorder = Color(0xFFFDE68A);
  static const Color criticalBorder = Color(0xFFFECACA);
  static const Color aiBorder = Color(0xFFC7D2FE);
  static const Color primaryBorder = Color(0xFFBFDBFE);

  // Typography Tokens
  static const Color textPrimary = Color(0xFF0F172A);     // Dark Navy Primary Text (#0F172A)
  static const Color textSecondary = Color(0xFF64748B);   // Slate Gray Secondary Text (#64748B)
  static const Color textSubtle = Color(0xFF94A3B8);      // Subtle Gray (#94A3B8)

  // Spacing Grid
  static const double space4 = 4.0;
  static const double space8 = 8.0;
  static const double space12 = 12.0;
  static const double space16 = 16.0;
  static const double space20 = 20.0;
  static const double space24 = 24.0;
  static const double space32 = 32.0;

  // Touch Target
  static const double minTouchTarget = 48.0;

  // Border Radii
  static final BorderRadius cardRadius = BorderRadius.circular(16);
  static final BorderRadius cardRadiusLg = BorderRadius.circular(20);
  static final BorderRadius pillRadius = BorderRadius.circular(24);
  static final BorderRadius buttonRadius = BorderRadius.circular(12);

  // Typography Styles (Inter)
  static TextStyle get display => GoogleFonts.inter(
    fontSize: 30,
    fontWeight: FontWeight.w800,
    color: textPrimary,
    letterSpacing: -0.5,
  );

  static TextStyle get screenTitle => GoogleFonts.inter(
    fontSize: 22,
    fontWeight: FontWeight.w700,
    color: textPrimary,
    letterSpacing: -0.3,
  );

  static TextStyle get sectionHeading => GoogleFonts.inter(
    fontSize: 16,
    fontWeight: FontWeight.w700,
    color: textPrimary,
    letterSpacing: 0.1,
  );

  static TextStyle get body => GoogleFonts.inter(
    fontSize: 14,
    fontWeight: FontWeight.w400,
    color: textPrimary,
    height: 1.45,
  );

  static TextStyle get bodySecondary => GoogleFonts.inter(
    fontSize: 13,
    fontWeight: FontWeight.w400,
    color: textSecondary,
    height: 1.4,
  );

  static TextStyle get metadata => GoogleFonts.inter(
    fontSize: 12,
    fontWeight: FontWeight.w500,
    color: textSecondary,
  );

  static TextStyle get primaryMetric => GoogleFonts.inter(
    fontSize: 32,
    fontWeight: FontWeight.w800,
    color: textPrimary,
    letterSpacing: -0.5,
  );

  // Box Shadows
  static List<BoxShadow> get subtleShadow => [
    BoxShadow(
      color: const Color(0xFF0F172A).withOpacity(0.04),
      blurRadius: 8,
      offset: const Offset(0, 2),
    ),
  ];
}

/// Reusable Mobile Card container with consistent borders, dark background, and padding
class MobileCard extends StatelessWidget {
  final Widget child;
  final EdgeInsetsGeometry padding;
  final Color? backgroundColor;
  final Color? borderColor;
  final VoidCallback? onTap;

  const MobileCard({
    super.key,
    required this.child,
    this.padding = const EdgeInsets.all(16),
    this.backgroundColor,
    this.borderColor,
    this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    final decoration = BoxDecoration(
      color: backgroundColor ?? MobileTheme.surface,
      borderRadius: MobileTheme.cardRadius,
      border: Border.all(color: borderColor ?? MobileTheme.border, width: 1),
      boxShadow: MobileTheme.subtleShadow,
    );

    if (onTap != null) {
      return Material(
        color: Colors.transparent,
        child: InkWell(
          onTap: onTap,
          borderRadius: MobileTheme.cardRadius,
          child: Container(
            padding: padding,
            decoration: decoration,
            child: child,
          ),
        ),
      );
    }

    return Container(
      padding: padding,
      decoration: decoration,
      child: child,
    );
  }
}

/// Section header with title, subtitle, and optional trailing action
class MobileSectionHeader extends StatelessWidget {
  final String title;
  final String? subtitle;
  final Widget? trailing;
  final IconData? icon;
  final Color? iconColor;

  const MobileSectionHeader({
    super.key,
    required this.title,
    this.subtitle,
    this.trailing,
    this.icon,
    this.iconColor,
  });

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.center,
        children: [
          if (icon != null) ...[
            Icon(icon, size: 18, color: iconColor ?? MobileTheme.primary),
            const SizedBox(width: 8),
          ],
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  title,
                  style: GoogleFonts.inter(
                    color: MobileTheme.textPrimary,
                    fontSize: 15,
                    fontWeight: FontWeight.w700,
                    letterSpacing: 0.2,
                  ),
                ),
                if (subtitle != null) ...[
                  const SizedBox(height: 2),
                  Text(
                    subtitle!,
                    style: GoogleFonts.inter(
                      color: MobileTheme.textSecondary,
                      fontSize: 12,
                    ),
                  ),
                ],
              ],
            ),
          ),
          if (trailing != null) trailing!,
        ],
      ),
    );
  }
}

/// Compact badge pill for status, source, and trajectory indication
class MobileBadge extends StatelessWidget {
  final String label;
  final Color color;
  final IconData? icon;
  final double fontSize;

  const MobileBadge({
    super.key,
    required this.label,
    required this.color,
    this.icon,
    this.fontSize = 11,
  });

  @override
  Widget build(BuildContext context) {
    Color bg = color.withOpacity(0.12);
    Color borderColor = color.withOpacity(0.25);
    if (color == MobileTheme.stable) {
      bg = MobileTheme.successBg;
      borderColor = MobileTheme.successBorder;
    } else if (color == MobileTheme.drift || color == MobileTheme.warning) {
      bg = MobileTheme.warningBg;
      borderColor = MobileTheme.warningBorder;
    } else if (color == MobileTheme.critical) {
      bg = MobileTheme.criticalBg;
      borderColor = MobileTheme.criticalBorder;
    } else if (color == MobileTheme.primary) {
      bg = MobileTheme.primaryBg;
      borderColor = MobileTheme.primaryBorder;
    } else if (color == MobileTheme.geminiPurple || color == MobileTheme.secondaryAccent) {
      bg = MobileTheme.aiBg;
      borderColor = MobileTheme.aiBorder;
    }

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
      decoration: BoxDecoration(
        color: bg,
        borderRadius: MobileTheme.pillRadius,
        border: Border.all(color: borderColor, width: 1),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          if (icon != null) ...[
            Icon(icon, size: fontSize + 3, color: color),
            const SizedBox(width: 5),
          ],
          Flexible(
            child: Text(
              label,
              style: GoogleFonts.inter(
                color: color,
                fontSize: fontSize,
                fontWeight: FontWeight.w700,
                letterSpacing: 0.3,
              ),
              overflow: TextOverflow.ellipsis,
            ),
          ),
        ],
      ),
    );
  }
}

/// Standardized prototype medical disclaimer banner
class PrototypeDisclaimerBanner extends StatelessWidget {
  final String text;

  const PrototypeDisclaimerBanner({
    super.key,
    this.text = 'Prototype • Not a medical diagnosis',
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
      decoration: BoxDecoration(
        color: MobileTheme.surfaceElevated,
        borderRadius: BorderRadius.circular(10),
        border: Border.all(color: MobileTheme.border, width: 1),
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.center,
        children: [
          const Icon(Icons.info_outline_rounded, color: MobileTheme.primary, size: 15),
          const SizedBox(width: 8),
          Expanded(
            child: Text(
              text,
              style: GoogleFonts.inter(
                color: MobileTheme.textSecondary,
                fontSize: 11,
                fontWeight: FontWeight.w500,
              ),
            ),
          ),
        ],
      ),
    );
  }
}
