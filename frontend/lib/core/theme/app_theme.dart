import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

/// OS4All Unified Light Healthcare-Tech Theme
/// Clean light background (#F6F9FC) + Blue healthcare-tech branding (#2563EB)
/// Clean white cards (#FFFFFF) + soft blue accents (#DBEAFE / #EFF6FF)
/// Primary Text: #0F172A | Secondary Text: #64748B | Subtle: #94A3B8
class AppTheme {
  // Foundations
  static const Color background = Color(0xFFF6F9FC);       // Clean Light Clinical Background (#F6F9FC)
  static const Color surface = Color(0xFFFFFFFF);          // Primary Card Surface (#FFFFFF)
  static const Color surfaceElevated = Color(0xFFF8FBFF);  // Secondary Elevated Surface (#F8FBFF)
  static const Color surfaceLight = Color(0xFFF8FBFF);     // Secondary Surface
  static const Color border = Color(0xFFE2E8F0);           // Card Border (#E2E8F0)
  static const Color borderLight = Color(0xFFF1F5F9);      // Subtle Border (#F1F5F9)
  static const Color cardBg = Color(0xFFFFFFFF);           // Surface Card Background (#FFFFFF)
  static const Color cardBorder = Color(0xFFE2E8F0);       // Surface Card Border (#E2E8F0)

  // Accents & Telemetry States
  static const Color primary = Color(0xFF2563EB);          // Primary Brand Blue (#2563EB)
  static const Color primaryDark = Color(0xFF1D4ED8);      // Dark Blue (#1D4ED8)
  static const Color softBlue = Color(0xFFDBEAFE);         // Soft Blue (#DBEAFE)
  static const Color lightBlue = Color(0xFFEFF6FF);        // Very Light Blue (#EFF6FF)
  static const Color secondary = Color(0xFF4F46E5);        // Secondary Accent: Indigo/AI (#4F46E5)
  static const Color stable = Color(0xFF16A34A);           // Stable: Emerald Green (#16A34A)
  static const Color attention = Color(0xFF2563EB);        // Informational Blue (#2563EB)
  static const Color critical = Color(0xFFDC2626);         // Critical: Coral Red (#DC2626)
  static const Color success = Color(0xFF16A34A);          // Green (#16A34A)
  static const Color warning = Color(0xFFD97706);          // Drift / Warning: Amber Gold (#D97706)
  static const Color drift = Color(0xFFD97706);            // Drift (#D97706)
  static const Color danger = Color(0xFFDC2626);           // Red (#DC2626)
  static const Color info = Color(0xFF2563EB);             // Brand Blue

  // Typography Tokens
  static const Color textPrimary = Color(0xFF0F172A);      // Dark Navy Text (#0F172A)
  static const Color textSecondary = Color(0xFF64748B);    // Muted Slate (#64748B)
  static const Color textMuted = Color(0xFF94A3B8);        // Subtle Gray (#94A3B8)

  // Compatibility Aliases for OS4All Widgets
  static const Color cyan = primary;
  static const Color cyanMuted = softBlue;
  static const Color indigo = secondary;
  static const Color violet = secondary;
  static const Color primaryCyan = primary;
  static const Color cyanAccent = primary;
  static const Color medicalTeal = primary;
  static const Color vitalityGreen = stable;
  static const Color warningAmber = warning;
  static const Color criticalCoral = critical;
  static const Color accentIndigo = secondary;
  static const Color accentPurple = secondary;
  static const Color electricBlue = primary;
  static const Color primaryBackground = background;
  static const Color surfaceCard = surface;
  static const Color surfaceCardLight = surfaceElevated;
  static const Color surfaceCardElevated = surfaceElevated;
  static const Color surfaceBorder = border;
  static const Color surfaceBorderLight = borderLight;
  static const Color surfaceMuted = surfaceElevated;
  static const Color textLight = textSecondary;

  // Gradients
  static const LinearGradient primaryGradient = LinearGradient(
    colors: [Color(0xFF2563EB), Color(0xFF3B82F6)],
    begin: Alignment.topLeft,
    end: Alignment.bottomRight,
  );

  static const LinearGradient cardGradient = LinearGradient(
    colors: [Color(0xFFFFFFFF), Color(0xFFFFFFFF)],
    begin: Alignment.topLeft,
    end: Alignment.bottomRight,
  );

  static const LinearGradient darkCardGradient = LinearGradient(
    colors: [Color(0xFFF8FBFF), Color(0xFFFFFFFF)],
    begin: Alignment.topLeft,
    end: Alignment.bottomRight,
  );

  // Spacing & Border Radii
  static const double radiusSmall = 8.0;
  static const double radiusMedium = 12.0;
  static const double radiusLarge = 16.0;
  static const double radiusCard = 16.0;

  static const double paddingSmall = 8.0;
  static const double paddingMedium = 16.0;
  static const double paddingLarge = 24.0;

  // Shadows
  static List<BoxShadow> get cardShadow => [
    BoxShadow(
      color: const Color(0xFF0F172A).withOpacity(0.04),
      blurRadius: 8,
      offset: const Offset(0, 2),
    ),
  ];

  static List<BoxShadow> get glowPrimary => [
    BoxShadow(
      color: primary.withOpacity(0.15),
      blurRadius: 10,
      offset: const Offset(0, 3),
    ),
  ];

  static ThemeData get theme => lightTheme;

  static ThemeData get lightTheme {
    final baseTextTheme = GoogleFonts.interTextTheme(ThemeData.light().textTheme);
    return ThemeData(
      useMaterial3: true,
      brightness: Brightness.light,
      scaffoldBackgroundColor: background,
      primaryColor: primary,
      cardColor: surface,
      colorScheme: const ColorScheme.light(
        primary: primary,
        secondary: secondary,
        surface: surface,
        error: critical,
        onPrimary: Colors.white,
        onSurface: textPrimary,
      ),
      textTheme: baseTextTheme.copyWith(
        displayLarge: GoogleFonts.inter(
          fontSize: 30,
          fontWeight: FontWeight.w800,
          color: textPrimary,
          letterSpacing: -0.5,
        ),
        headlineLarge: GoogleFonts.inter(
          fontSize: 24,
          fontWeight: FontWeight.w800,
          color: textPrimary,
          letterSpacing: -0.4,
        ),
        headlineMedium: GoogleFonts.inter(
          fontSize: 20,
          fontWeight: FontWeight.w700,
          color: textPrimary,
          letterSpacing: -0.3,
        ),
        titleLarge: GoogleFonts.inter(
          fontSize: 17,
          fontWeight: FontWeight.w700,
          color: textPrimary,
        ),
        titleMedium: GoogleFonts.inter(
          fontSize: 15,
          fontWeight: FontWeight.w600,
          color: textPrimary,
        ),
        titleSmall: GoogleFonts.inter(
          fontSize: 13,
          fontWeight: FontWeight.w600,
          color: textPrimary,
        ),
        bodyLarge: GoogleFonts.inter(
          fontSize: 14,
          fontWeight: FontWeight.w400,
          color: textPrimary,
          height: 1.5,
        ),
        bodyMedium: GoogleFonts.inter(
          fontSize: 13,
          fontWeight: FontWeight.w400,
          color: textSecondary,
          height: 1.45,
        ),
        labelLarge: GoogleFonts.inter(
          fontSize: 12,
          fontWeight: FontWeight.w700,
          letterSpacing: 0.3,
          color: textPrimary,
        ),
        labelSmall: GoogleFonts.inter(
          fontSize: 11,
          fontWeight: FontWeight.w600,
          letterSpacing: 0.4,
          color: textMuted,
        ),
      ),
      appBarTheme: AppBarTheme(
        backgroundColor: surface,
        elevation: 0,
        centerTitle: false,
        iconTheme: const IconThemeData(color: textPrimary),
        titleTextStyle: GoogleFonts.inter(
          color: textPrimary,
          fontSize: 18,
          fontWeight: FontWeight.w700,
          letterSpacing: -0.2,
        ),
      ),
      cardTheme: CardThemeData(
        color: surface,
        elevation: 0,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(16),
          side: const BorderSide(color: border, width: 1),
        ),
      ),
      elevatedButtonTheme: ElevatedButtonThemeData(
        style: ElevatedButton.styleFrom(
          backgroundColor: primary,
          foregroundColor: Colors.white,
          elevation: 0,
          minimumSize: const Size(double.infinity, 48),
          padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 14),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(12),
          ),
          textStyle: GoogleFonts.inter(
            fontSize: 14,
            fontWeight: FontWeight.w700,
            letterSpacing: 0.2,
          ),
        ),
      ),
      inputDecorationTheme: InputDecorationTheme(
        filled: true,
        fillColor: surfaceElevated,
        hintStyle: GoogleFonts.inter(color: textMuted, fontSize: 13),
        labelStyle: GoogleFonts.inter(color: textSecondary, fontSize: 13),
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
          borderSide: const BorderSide(color: border),
        ),
        enabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
          borderSide: const BorderSide(color: border),
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
          borderSide: const BorderSide(color: primary, width: 1.5),
        ),
      ),
    );
  }

  static ThemeData get darkTheme => lightTheme;
}
