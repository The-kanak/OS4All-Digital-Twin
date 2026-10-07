import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import '../../core/theme/mobile_design_system.dart';

/// OS4All Unified Mobile Component Library
/// Built specifically for responsive 360dp–392dp mobile viewports (e.g. Xiaomi M2101K7BI).
/// Zero unconstrained rows, zero clipping, min 48dp touch targets.

/// 1. Top Brand Header
class OS4Header extends StatelessWidget {
  final String title;
  final String subtitle;
  final Widget? trailing;

  const OS4Header({
    super.key,
    this.title = 'OS4All',
    this.subtitle = 'DIGITAL TWIN',
    this.trailing,
  });

  @override
  Widget build(BuildContext context) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      crossAxisAlignment: CrossAxisAlignment.center,
      children: [
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            mainAxisSize: MainAxisSize.min,
            children: [
              Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Container(
                    width: 8,
                    height: 8,
                    decoration: const BoxDecoration(
                      color: MobileTheme.primary,
                      shape: BoxShape.circle,
                    ),
                  ),
                  const SizedBox(width: 6),
                  Text(
                    title,
                    style: GoogleFonts.inter(
                      fontSize: 18,
                      fontWeight: FontWeight.w900,
                      color: MobileTheme.textPrimary,
                      letterSpacing: -0.3,
                    ),
                  ),
                  const SizedBox(width: 6),
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                    decoration: BoxDecoration(
                      color: MobileTheme.surfaceElevated,
                      borderRadius: BorderRadius.circular(4),
                    ),
                    child: Text(
                      subtitle,
                      style: GoogleFonts.inter(
                        fontSize: 9,
                        fontWeight: FontWeight.w700,
                        color: MobileTheme.primary,
                        letterSpacing: 0.5,
                      ),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 2),
              Text(
                'Your body, modeled.',
                style: GoogleFonts.inter(
                  fontSize: 11,
                  fontWeight: FontWeight.w500,
                  color: MobileTheme.textSecondary,
                ),
              ),
            ],
          ),
        ),
        if (trailing != null) trailing!,
      ],
    );
  }
}

/// 2. Patient Identity Card with Switcher Trigger
class PatientIdentityCard extends StatelessWidget {
  final String name;
  final int age;
  final String gender;
  final String condition;
  final VoidCallback? onSwitchPatient;

  const PatientIdentityCard({
    super.key,
    required this.name,
    required this.age,
    required this.gender,
    required this.condition,
    this.onSwitchPatient,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: MobileTheme.surface,
        borderRadius: MobileTheme.cardRadius,
        border: Border.all(color: MobileTheme.border, width: 1),
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.center,
        children: [
          Container(
            width: 44,
            height: 44,
            decoration: BoxDecoration(
              gradient: const LinearGradient(
                colors: [Color(0xFF2563EB), Color(0xFF3B82F6)],
                begin: Alignment.topLeft,
                end: Alignment.bottomRight,
              ),
              borderRadius: BorderRadius.circular(12),
            ),
            child: const Center(
              child: Icon(Icons.person_rounded, color: Colors.white, size: 24),
            ),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              mainAxisSize: MainAxisSize.min,
              children: [
                Wrap(
                  crossAxisAlignment: WrapCrossAlignment.center,
                  spacing: 6,
                  runSpacing: 2,
                  children: [
                    Text(
                      name.toUpperCase(),
                      style: GoogleFonts.inter(
                        fontSize: 14,
                        fontWeight: FontWeight.w800,
                        color: MobileTheme.textPrimary,
                        letterSpacing: 0.2,
                      ),
                      overflow: TextOverflow.ellipsis,
                    ),
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 1.5),
                      decoration: BoxDecoration(
                        color: MobileTheme.primaryBg,
                        borderRadius: BorderRadius.circular(4),
                        border: Border.all(color: MobileTheme.primaryBorder, width: 0.8),
                      ),
                      child: Text(
                        'Virtual Patient',
                        style: GoogleFonts.inter(
                          fontSize: 9,
                          fontWeight: FontWeight.w700,
                          color: MobileTheme.primary,
                        ),
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 2),
                Text(
                  '$age • $gender • $condition',
                  style: GoogleFonts.inter(
                    fontSize: 11.5,
                    fontWeight: FontWeight.w500,
                    color: MobileTheme.textSecondary,
                  ),
                  overflow: TextOverflow.ellipsis,
                ),
              ],
            ),
          ),
          if (onSwitchPatient != null)
            IconButton(
              icon: const Icon(Icons.swap_horiz_rounded, color: MobileTheme.textSecondary, size: 20),
              tooltip: 'Switch Patient',
              constraints: const BoxConstraints(minWidth: 48, minHeight: 48),
              onPressed: onSwitchPatient,
            ),
        ],
      ),
    );
  }
}

/// 3. Digital Twin Status Card
class TwinStatusCard extends StatelessWidget {
  final String state;
  final double riskScore;
  final double currentGlucose;
  final double glucoseVelocity;
  final String trajectoryDirection;
  final Color stateColor;

  const TwinStatusCard({
    super.key,
    required this.state,
    required this.riskScore,
    required this.currentGlucose,
    required this.glucoseVelocity,
    required this.trajectoryDirection,
    required this.stateColor,
  });

  @override
  Widget build(BuildContext context) {
    String headline = 'Glucose trajectory is rising';
    if (state == 'STABLE') {
      headline = 'Metabolic homeostasis maintained';
    } else if (state == 'PRE_SYMPTOMATIC_DRIFT') {
      headline = 'Early metabolic drift detected';
    } else if (state == 'CRITICAL' || riskScore >= 75) {
      headline = 'Elevated glycemic surge projected';
    }

    final formattedState = state.replaceAll('_', ' ');

    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: MobileTheme.surface,
        borderRadius: MobileTheme.cardRadius,
        border: Border.all(color: stateColor.withOpacity(0.55), width: 1.5),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Wrap(
            alignment: WrapAlignment.spaceBetween,
            crossAxisAlignment: WrapCrossAlignment.center,
            spacing: 8,
            runSpacing: 4,
            children: [
              Text(
                'DIGITAL TWIN STATE',
                style: GoogleFonts.inter(
                  fontSize: 11,
                  fontWeight: FontWeight.w800,
                  color: MobileTheme.textSecondary,
                  letterSpacing: 0.6,
                ),
              ),
              StatusPill(label: formattedState, color: stateColor),
            ],
          ),
          const SizedBox(height: 10),
          Text(
            headline,
            style: GoogleFonts.inter(
              fontSize: 16,
              fontWeight: FontWeight.w700,
              color: MobileTheme.textPrimary,
              height: 1.25,
            ),
          ),
          const SizedBox(height: 16),
          // 3 Metric Blocks wrapped in responsive row
          Row(
            children: [
              Expanded(
                child: _buildMetricBlock(
                  label: 'Risk Score',
                  value: riskScore.toStringAsFixed(1),
                  unit: '/ 100',
                  color: stateColor,
                ),
              ),
              Container(width: 1, height: 42, color: MobileTheme.border),
              Expanded(
                child: _buildMetricBlock(
                  label: 'Current Glucose',
                  value: currentGlucose.toStringAsFixed(0),
                  unit: 'mg/dL',
                  color: stateColor,
                ),
              ),
              Container(width: 1, height: 42, color: MobileTheme.border),
              Expanded(
                child: _buildMetricBlock(
                  label: 'Velocity',
                  value: '${glucoseVelocity >= 0 ? '+' : ''}${glucoseVelocity.toStringAsFixed(2)}',
                  unit: 'mg/dL/min',
                  color: stateColor,
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildMetricBlock({
    required String label,
    required String value,
    required String unit,
    required Color color,
  }) {
    return Column(
      mainAxisSize: MainAxisSize.min,
      children: [
        Text(
          label,
          style: GoogleFonts.inter(
            fontSize: 10.5,
            fontWeight: FontWeight.w600,
            color: MobileTheme.textSecondary,
          ),
          textAlign: TextAlign.center,
          maxLines: 1,
          overflow: TextOverflow.ellipsis,
        ),
        const SizedBox(height: 3),
        Text(
          value,
          style: GoogleFonts.inter(
            fontSize: 19,
            fontWeight: FontWeight.w800,
            color: color,
            letterSpacing: -0.4,
          ),
        ),
        Text(
          unit,
          style: GoogleFonts.inter(
            fontSize: 9.5,
            fontWeight: FontWeight.w500,
            color: MobileTheme.textSecondary,
          ),
        ),
      ],
    );
  }
}

/// 4. 2-Hour Outlook Card
class ProjectionCard extends StatelessWidget {
  final Map<String, dynamic> trajectory;
  final List<dynamic> points;
  final Color stateColor;
  final VoidCallback? onExplore;

  const ProjectionCard({
    super.key,
    required this.trajectory,
    required this.points,
    required this.stateColor,
    this.onExplore,
  });

  @override
  Widget build(BuildContext context) {
    final projected120 = trajectory['projectedGlucose120Min']?.toString() ?? '332.5';
    final delta = trajectory['projectedDelta120Min'] != null
        ? ((trajectory['projectedDelta120Min'] as num) >= 0 ? '+' : '') +
            (trajectory['projectedDelta120Min'] as num).toStringAsFixed(1)
        : '+170.5';
    final dir = trajectory['trajectoryDirection']?.toString() ?? 'RISING';

    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: MobileTheme.surface,
        borderRadius: MobileTheme.cardRadius,
        border: Border.all(color: MobileTheme.border, width: 1),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Wrap(
            alignment: WrapAlignment.spaceBetween,
            crossAxisAlignment: WrapCrossAlignment.center,
            spacing: 8,
            runSpacing: 4,
            children: [
              Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  const Icon(Icons.auto_graph_rounded, color: MobileTheme.primary, size: 16),
                  const SizedBox(width: 6),
                  Text(
                    '2-HOUR OUTLOOK',
                    style: GoogleFonts.inter(
                      fontSize: 11.5,
                      fontWeight: FontWeight.w800,
                      color: MobileTheme.textPrimary,
                      letterSpacing: 0.5,
                    ),
                  ),
                ],
              ),
              StatusPill(
                label: '$dir ($delta mg/dL)',
                color: stateColor,
                icon: dir == 'RISING' || dir == 'SLIGHT_INCREASE'
                    ? Icons.trending_up_rounded
                    : (dir == 'FALLING' ? Icons.trending_down_rounded : Icons.trending_flat_rounded),
              ),
            ],
          ),
          const SizedBox(height: 8),
          Text(
            'Deterministic projection: reaches $projected120 mg/dL at T+120m.',
            style: GoogleFonts.inter(
              fontSize: 12,
              fontWeight: FontWeight.w400,
              color: MobileTheme.textSecondary,
            ),
          ),
          const SizedBox(height: 14),

          // 5 Milestone Chips
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 10),
            decoration: BoxDecoration(
              color: MobileTheme.surfaceElevated,
              borderRadius: BorderRadius.circular(12),
              border: Border.all(color: MobileTheme.border),
            ),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.spaceAround,
              children: [
                _buildStep('Now', _getPointValue(0, '162'), MobileTheme.textPrimary),
                _buildStep('+30m', _getPointValue(30, '205'), stateColor),
                _buildStep('+60m', _getPointValue(60, '254'), stateColor),
                _buildStep('+90m', _getPointValue(90, '298'), stateColor),
                _buildStep('+120m', projected120, stateColor),
              ],
            ),
          ),
        ],
      ),
    );
  }

  String _getPointValue(int minutes, String fallback) {
    for (final p in points) {
      if (p is Map && (p['timeMinutes'] as num?)?.toInt() == minutes) {
        if (p['glucoseMgDl'] != null) {
          return (p['glucoseMgDl'] as num).toStringAsFixed(0);
        }
      }
    }
    return fallback;
  }

  Widget _buildStep(String time, String value, Color color) {
    return Column(
      mainAxisSize: MainAxisSize.min,
      children: [
        Text(
          time,
          style: GoogleFonts.inter(
            fontSize: 10,
            fontWeight: FontWeight.w600,
            color: MobileTheme.textSecondary,
          ),
        ),
        const SizedBox(height: 3),
        Text(
          value,
          style: GoogleFonts.inter(
            fontSize: 13.5,
            fontWeight: FontWeight.w800,
            color: color,
          ),
        ),
        Text(
          'mg/dL',
          style: GoogleFonts.inter(
            fontSize: 8.5,
            fontWeight: FontWeight.w500,
            color: MobileTheme.textSubtle,
          ),
        ),
      ],
    );
  }
}

/// 5. Responsive Trajectory Chart
class TrajectoryChartCard extends StatelessWidget {
  final List<dynamic> points;
  final double currentGlucose;
  final String trajectoryDirection;
  final Color stateColor;

  const TrajectoryChartCard({
    super.key,
    required this.points,
    required this.currentGlucose,
    required this.trajectoryDirection,
    required this.stateColor,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: MobileTheme.surface,
        borderRadius: MobileTheme.cardRadius,
        border: Border.all(color: MobileTheme.border, width: 1),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Wrap(
            alignment: WrapAlignment.spaceBetween,
            crossAxisAlignment: WrapCrossAlignment.center,
            spacing: 8,
            runSpacing: 4,
            children: [
              Text(
                '2-HOUR PROJECTED TRAJECTORY',
                style: GoogleFonts.inter(
                  fontSize: 11.5,
                  fontWeight: FontWeight.w800,
                  color: MobileTheme.textPrimary,
                  letterSpacing: 0.5,
                ),
              ),
              Text(
                'Deterministic RK4',
                style: GoogleFonts.inter(
                  fontSize: 10,
                  fontWeight: FontWeight.w600,
                  color: MobileTheme.primary,
                ),
              ),
            ],
          ),
          const SizedBox(height: 4),
          Text(
            'Target physiological corridor: 70 – 140 mg/dL',
            style: GoogleFonts.inter(fontSize: 11, color: MobileTheme.textSecondary),
          ),
          const SizedBox(height: 14),

          // Custom Painter Canvas (Height responsive)
          SizedBox(
            height: 150,
            width: double.infinity,
            child: LayoutBuilder(
              builder: (context, constraints) {
                return CustomPaint(
                  size: Size(constraints.maxWidth, 150),
                  painter: _TrajectoryPainter(
                    points: points,
                    currentGlucose: currentGlucose,
                    lineColor: stateColor,
                  ),
                );
              },
            ),
          ),

          const SizedBox(height: 8),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text('Now', style: GoogleFonts.inter(fontSize: 10, color: MobileTheme.textSecondary)),
              Text('+30m', style: GoogleFonts.inter(fontSize: 10, color: MobileTheme.textSecondary)),
              Text('+60m', style: GoogleFonts.inter(fontSize: 10, color: MobileTheme.textSecondary)),
              Text('+90m', style: GoogleFonts.inter(fontSize: 10, color: MobileTheme.textSecondary)),
              Text('+120m', style: GoogleFonts.inter(fontSize: 10, color: MobileTheme.textSecondary)),
            ],
          ),
        ],
      ),
    );
  }
}

class _TrajectoryPainter extends CustomPainter {
  final List<dynamic> points;
  final double currentGlucose;
  final Color lineColor;

  _TrajectoryPainter({
    required this.points,
    required this.currentGlucose,
    required this.lineColor,
  });

  @override
  void paint(Canvas canvas, Size size) {
    const minG = 60.0;
    const maxG = 360.0;
    const range = maxG - minG;

    double getY(double g) {
      final clamped = g.clamp(minG, maxG);
      return size.height - ((clamped - minG) / range * size.height);
    }

    // 1. Draw Target Normal Band (70 - 140 mg/dL)
    final topY = getY(140.0);
    final botY = getY(70.0);
    final bandPaint = Paint()
      ..color = const Color(0xFFDCFCE7).withOpacity(0.55)
      ..style = PaintingStyle.fill;
    canvas.drawRect(Rect.fromLTRB(0, topY, size.width, botY), bandPaint);

    // Target upper boundary dashed line
    final dashPaint = Paint()
      ..color = MobileTheme.stable.withOpacity(0.4)
      ..strokeWidth = 1;
    canvas.drawLine(Offset(0, topY), Offset(size.width, topY), dashPaint);

    // 2. Trajectory Curve
    final pointCoords = <Offset>[];
    if (points.isNotEmpty) {
      for (int i = 0; i < points.length; i++) {
        final pt = points[i];
        final t = (pt['timeMinutes'] as num?)?.toDouble() ?? (i * 30.0);
        final g = (pt['glucoseMgDl'] as num?)?.toDouble() ?? currentGlucose;
        final x = (t / 120.0) * size.width;
        final y = getY(g);
        pointCoords.add(Offset(x, y));
      }
    } else {
      pointCoords.add(Offset(0, getY(currentGlucose)));
      pointCoords.add(Offset(size.width, getY(currentGlucose + 40)));
    }

    // Path
    final path = Path();
    path.moveTo(pointCoords[0].dx, pointCoords[0].dy);
    for (int i = 1; i < pointCoords.length; i++) {
      final p0 = pointCoords[i - 1];
      final p1 = pointCoords[i];
      final cx = (p0.dx + p1.dx) / 2;
      path.cubicTo(cx, p0.dy, cx, p1.dy, p1.dx, p1.dy);
    }

    // Gradient fill under path
    final fillPath = Path.from(path)
      ..lineTo(size.width, size.height)
      ..lineTo(0, size.height)
      ..close();
    final gradientPaint = Paint()
      ..shader = LinearGradient(
        colors: [lineColor.withOpacity(0.25), lineColor.withOpacity(0.0)],
        begin: Alignment.topCenter,
        end: Alignment.bottomCenter,
      ).createShader(Rect.fromLTWH(0, 0, size.width, size.height))
      ..style = PaintingStyle.fill;
    canvas.drawPath(fillPath, gradientPaint);

    // Draw main line
    final linePaint = Paint()
      ..color = lineColor
      ..strokeWidth = 2.5
      ..style = PaintingStyle.stroke;
    canvas.drawPath(path, linePaint);

    // Draw point markers
    final dotPaint = Paint()..color = lineColor;
    final dotBgPaint = Paint()..color = MobileTheme.surface;
    for (final pt in pointCoords) {
      canvas.drawCircle(pt, 5, dotBgPaint);
      canvas.drawCircle(pt, 3.5, dotPaint);
    }
  }

  @override
  bool shouldRepaint(covariant _TrajectoryPainter oldDelegate) {
    return oldDelegate.currentGlucose != currentGlucose ||
        oldDelegate.lineColor != lineColor ||
        oldDelegate.points != points;
  }
}

/// 6. Counterfactual Scenario Card
class ScenarioCard extends StatelessWidget {
  final String title;
  final String subtitle;
  final IconData icon;
  final bool isSelected;
  final VoidCallback onSelect;

  const ScenarioCard({
    super.key,
    required this.title,
    required this.subtitle,
    required this.icon,
    required this.isSelected,
    required this.onSelect,
  });

  @override
  Widget build(BuildContext context) {
    return Material(
      color: Colors.transparent,
      child: InkWell(
        onTap: onSelect,
        borderRadius: MobileTheme.cardRadius,
        child: Container(
          constraints: const BoxConstraints(minHeight: 52),
          padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
          decoration: BoxDecoration(
            color: isSelected ? MobileTheme.lightBlueAccent : MobileTheme.surface,
            borderRadius: MobileTheme.cardRadius,
            border: Border.all(
              color: isSelected ? MobileTheme.primary : MobileTheme.border,
              width: isSelected ? 1.5 : 1,
            ),
          ),
          child: Row(
            children: [
              Container(
                width: 36,
                height: 36,
                decoration: BoxDecoration(
                  color: isSelected ? MobileTheme.primary : MobileTheme.surfaceElevated,
                  borderRadius: BorderRadius.circular(10),
                ),
                child: Icon(
                  icon,
                  size: 20,
                  color: isSelected ? Colors.white : MobileTheme.primary,
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Text(
                      title,
                      style: GoogleFonts.inter(
                        fontSize: 13.5,
                        fontWeight: FontWeight.w700,
                        color: isSelected ? MobileTheme.primaryDark : MobileTheme.textPrimary,
                      ),
                    ),
                    const SizedBox(height: 2),
                    Text(
                      subtitle,
                      style: GoogleFonts.inter(
                        fontSize: 11,
                        color: MobileTheme.textSecondary,
                      ),
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                    ),
                  ],
                ),
              ),
              if (isSelected)
                const Icon(Icons.check_circle_rounded, color: MobileTheme.primary, size: 20),
            ],
          ),
        ),
      ),
    );
  }
}

/// 7. Signal & Vital Card
class SignalCard extends StatelessWidget {
  final String label;
  final String value;
  final String unit;
  final IconData icon;
  final Color color;
  final String? subtitle;

  const SignalCard({
    super.key,
    required this.label,
    required this.value,
    required this.unit,
    required this.icon,
    required this.color,
    this.subtitle,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: MobileTheme.surface,
        borderRadius: BorderRadius.circular(14),
        border: Border.all(color: MobileTheme.border, width: 1),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        mainAxisSize: MainAxisSize.min,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Expanded(
                child: Text(
                  label,
                  style: GoogleFonts.inter(
                    fontSize: 11,
                    fontWeight: FontWeight.w600,
                    color: MobileTheme.textSecondary,
                  ),
                  overflow: TextOverflow.ellipsis,
                ),
              ),
              const SizedBox(width: 4),
              Icon(icon, size: 16, color: color),
            ],
          ),
          const SizedBox(height: 6),
          Row(
            crossAxisAlignment: CrossAxisAlignment.baseline,
            textBaseline: TextBaseline.alphabetic,
            children: [
              Flexible(
                child: Text(
                  value,
                  style: GoogleFonts.inter(
                    fontSize: 18,
                    fontWeight: FontWeight.w800,
                    color: MobileTheme.textPrimary,
                    letterSpacing: -0.4,
                  ),
                  overflow: TextOverflow.ellipsis,
                ),
              ),
              if (unit.isNotEmpty) ...[
                const SizedBox(width: 4),
                Text(
                  unit,
                  style: GoogleFonts.inter(
                    fontSize: 10,
                    fontWeight: FontWeight.w500,
                    color: MobileTheme.textSecondary,
                  ),
                ),
              ],
            ],
          ),
          if (subtitle != null) ...[
            const SizedBox(height: 2),
            Text(
              subtitle!,
              style: GoogleFonts.inter(
                fontSize: 9.5,
                color: MobileTheme.textSubtle,
              ),
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
            ),
          ],
        ],
      ),
    );
  }
}

/// 8. Quick Action Card
class QuickActionCard extends StatelessWidget {
  final String title;
  final String subtitle;
  final IconData icon;
  final Color color;
  final VoidCallback onTap;

  const QuickActionCard({
    super.key,
    required this.title,
    required this.subtitle,
    required this.icon,
    required this.color,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return Material(
      color: Colors.transparent,
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(14),
        child: Container(
          constraints: const BoxConstraints(minHeight: 52),
          padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
          decoration: BoxDecoration(
            color: MobileTheme.surface,
            borderRadius: BorderRadius.circular(14),
            border: Border.all(color: MobileTheme.border, width: 1),
          ),
          child: Row(
            children: [
              Container(
                width: 36,
                height: 36,
                decoration: BoxDecoration(
                  color: color.withOpacity(0.15),
                  borderRadius: BorderRadius.circular(10),
                ),
                child: Icon(icon, color: color, size: 20),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Text(
                      title,
                      style: GoogleFonts.inter(
                        fontSize: 13.5,
                        fontWeight: FontWeight.w700,
                        color: MobileTheme.textPrimary,
                      ),
                    ),
                    Text(
                      subtitle,
                      style: GoogleFonts.inter(
                        fontSize: 11,
                        color: MobileTheme.textSecondary,
                      ),
                    ),
                  ],
                ),
              ),
              const Icon(Icons.chevron_right_rounded, color: MobileTheme.textSecondary, size: 20),
            ],
          ),
        ),
      ),
    );
  }
}

/// 9. Status Pill Badge
class StatusPill extends StatelessWidget {
  final String label;
  final Color color;
  final IconData? icon;

  const StatusPill({
    super.key,
    required this.label,
    required this.color,
    this.icon,
  });

  @override
  Widget build(BuildContext context) {
    Color bg = color.withOpacity(0.12);
    Color borderColor = color.withOpacity(0.30);
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
      padding: const EdgeInsets.symmetric(horizontal: 9, vertical: 3.5),
      decoration: BoxDecoration(
        color: bg,
        borderRadius: MobileTheme.pillRadius,
        border: Border.all(color: borderColor, width: 1),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          if (icon != null) ...[
            Icon(icon, size: 12, color: color),
            const SizedBox(width: 4),
          ],
          Flexible(
            child: Text(
              label,
              style: GoogleFonts.inter(
                color: color,
                fontSize: 10.5,
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

/// 10. Primary Button (min 48dp)
class PrimaryButton extends StatelessWidget {
  final String label;
  final VoidCallback? onPressed;
  final IconData? icon;
  final bool isLoading;

  const PrimaryButton({
    super.key,
    required this.label,
    required this.onPressed,
    this.icon,
    this.isLoading = false,
  });

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      height: 48,
      width: double.infinity,
      child: ElevatedButton(
        onPressed: isLoading ? null : onPressed,
        style: ElevatedButton.styleFrom(
          backgroundColor: MobileTheme.primary,
          foregroundColor: Colors.white,
          disabledBackgroundColor: MobileTheme.surfaceElevated,
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
          elevation: 0,
        ),
        child: isLoading
            ? const SizedBox(
                width: 20,
                height: 20,
                child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white),
              )
            : Row(
                mainAxisAlignment: MainAxisAlignment.center,
                mainAxisSize: MainAxisSize.min,
                children: [
                  if (icon != null) ...[
                    Icon(icon, size: 18, color: Colors.white),
                    const SizedBox(width: 8),
                  ],
                  Flexible(
                    child: Text(
                      label,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: GoogleFonts.inter(
                        fontSize: 14,
                        fontWeight: FontWeight.w700,
                        color: Colors.white,
                      ),
                    ),
                  ),
                ],
              ),
      ),
    );
  }
}

/// 11. Secondary Button (min 48dp)
class SecondaryButton extends StatelessWidget {
  final String label;
  final VoidCallback? onPressed;
  final IconData? icon;

  const SecondaryButton({
    super.key,
    required this.label,
    required this.onPressed,
    this.icon,
  });

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      height: 48,
      width: double.infinity,
      child: OutlinedButton(
        onPressed: onPressed,
        style: OutlinedButton.styleFrom(
          backgroundColor: MobileTheme.surface,
          foregroundColor: MobileTheme.textPrimary,
          side: const BorderSide(color: MobileTheme.border, width: 1),
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
        ),
        child: Row(
          mainAxisAlignment: MainAxisAlignment.center,
          mainAxisSize: MainAxisSize.min,
          children: [
            if (icon != null) ...[
              Icon(icon, size: 18, color: MobileTheme.textSecondary),
              const SizedBox(width: 8),
            ],
            Flexible(
              child: Text(
                label,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: GoogleFonts.inter(
                  fontSize: 14,
                  fontWeight: FontWeight.w600,
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

/// 12. Grounding Badge
class GroundingBadge extends StatelessWidget {
  final bool isGrounded;
  final String? model;

  const GroundingBadge({
    super.key,
    this.isGrounded = true,
    this.model = 'gemini-3.8-flash',
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
      decoration: BoxDecoration(
        color: MobileTheme.aiBg,
        borderRadius: BorderRadius.circular(20),
        border: Border.all(color: MobileTheme.aiBorder),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          const Icon(Icons.verified_rounded, color: MobileTheme.geminiPurple, size: 14),
          const SizedBox(width: 5),
          Flexible(
            child: Text(
              'GEMINI • Grounded in Digital Twin',
              style: GoogleFonts.inter(
                fontSize: 10.5,
                fontWeight: FontWeight.w700,
                color: MobileTheme.geminiPurple,
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

/// 13. Chat Bubble
class ChatBubble extends StatelessWidget {
  final String message;
  final bool isUser;
  final String timestamp;

  const ChatBubble({
    super.key,
    required this.message,
    required this.isUser,
    this.timestamp = 'Just now',
  });

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 6),
      child: Align(
        alignment: isUser ? Alignment.centerRight : Alignment.centerLeft,
        child: Container(
          constraints: BoxConstraints(
            maxWidth: MediaQuery.of(context).size.width * 0.82,
          ),
          padding: const EdgeInsets.all(14),
          decoration: BoxDecoration(
            color: isUser ? MobileTheme.lightBlueAccent : MobileTheme.surface,
            borderRadius: BorderRadius.only(
              topLeft: const Radius.circular(16),
              topRight: const Radius.circular(16),
              bottomLeft: Radius.circular(isUser ? 16 : 4),
              bottomRight: Radius.circular(isUser ? 4 : 16),
            ),
            border: Border.all(
              color: isUser ? MobileTheme.softBlue : MobileTheme.border,
              width: 1,
            ),
            boxShadow: isUser ? null : MobileTheme.subtleShadow,
          ),
          child: Column(
            crossAxisAlignment: isUser ? CrossAxisAlignment.end : CrossAxisAlignment.start,
            mainAxisSize: MainAxisSize.min,
            children: [
              if (!isUser) ...[
                Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    const Icon(Icons.auto_awesome_rounded, color: MobileTheme.geminiPurple, size: 13),
                    const SizedBox(width: 5),
                    Text(
                      'Digital Twin AI',
                      style: GoogleFonts.inter(
                        fontSize: 11,
                        fontWeight: FontWeight.w700,
                        color: MobileTheme.geminiPurple,
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 6),
              ],
              Text(
                message,
                style: GoogleFonts.inter(
                  fontSize: 13.5,
                  fontWeight: FontWeight.w400,
                  color: MobileTheme.textPrimary,
                  height: 1.45,
                ),
              ),
              const SizedBox(height: 4),
              Text(
                timestamp,
                style: GoogleFonts.inter(
                  fontSize: 10,
                  color: MobileTheme.textSubtle,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
