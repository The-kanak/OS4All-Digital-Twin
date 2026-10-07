import 'package:flutter/material.dart';
import '../../core/theme/app_theme.dart';

/// Interactive chart displaying continuous physiological signal data points
/// along with the user's personal baseline range envelope.
/// Includes support for loading, empty, and error states.
class BaselineSignalChart extends StatefulWidget {
  final String metricName;
  final String unit;
  final List<double> values;
  final List<String> labels;
  final double baselineMean;
  final double baselineLow;
  final double baselineHigh;
  final Color lineColor;
  final String state; // STABLE, DRIFT, ANOMALY, FOLLOW-UP
  final bool isLoading;
  final bool hasError;
  final String? errorMessage;
  final VoidCallback? onRetry;

  const BaselineSignalChart({
    super.key,
    required this.metricName,
    required this.unit,
    required this.values,
    required this.labels,
    required this.baselineMean,
    required this.baselineLow,
    required this.baselineHigh,
    this.lineColor = AppTheme.primary,
    this.state = 'STABLE',
    this.isLoading = false,
    this.hasError = false,
    this.errorMessage,
    this.onRetry,
  });

  @override
  State<BaselineSignalChart> createState() => _BaselineSignalChartState();
}

class _BaselineSignalChartState extends State<BaselineSignalChart> {
  int? _hoveredIndex;

  @override
  Widget build(BuildContext context) {
    if (widget.isLoading) {
      return _buildContainer(
        child: SizedBox(
          height: 180,
          child: Center(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                const SizedBox(
                  width: 28,
                  height: 28,
                  child: CircularProgressIndicator(strokeWidth: 2.5, color: AppTheme.primary),
                ),
                const SizedBox(height: 12),
                Text(
                  'Calibrating ${widget.metricName} baseline telemetry...',
                  style: const TextStyle(color: AppTheme.textSecondary, fontSize: 12, fontWeight: FontWeight.w500),
                ),
              ],
            ),
          ),
        ),
      );
    }

    if (widget.hasError) {
      return _buildContainer(
        child: SizedBox(
          height: 180,
          child: Center(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                const Icon(Icons.error_outline_rounded, color: AppTheme.danger, size: 28),
                const SizedBox(height: 8),
                Text(
                  widget.errorMessage ?? 'Unable to render baseline chart for ${widget.metricName}',
                  style: const TextStyle(color: AppTheme.textPrimary, fontSize: 13, fontWeight: FontWeight.w600),
                  textAlign: TextAlign.center,
                ),
                if (widget.onRetry != null) ...[
                  const SizedBox(height: 10),
                  TextButton.icon(
                    onPressed: widget.onRetry,
                    icon: const Icon(Icons.refresh_rounded, size: 16),
                    label: const Text('Retry Calibration'),
                    style: TextButton.styleFrom(
                      foregroundColor: AppTheme.primary,
                      textStyle: const TextStyle(fontSize: 12, fontWeight: FontWeight.w700),
                    ),
                  ),
                ],
              ],
            ),
          ),
        ),
      );
    }

    if (widget.values.isEmpty) {
      return _buildContainer(
        child: SizedBox(
          height: 180,
          child: Center(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                Icon(Icons.show_chart_rounded, color: AppTheme.textMuted.withValues(alpha: 0.5), size: 36),
                const SizedBox(height: 8),
                Text(
                  'No recent telemetry for ${widget.metricName}',
                  style: const TextStyle(color: AppTheme.textSecondary, fontSize: 13, fontWeight: FontWeight.w600),
                ),
                const SizedBox(height: 4),
                const Text(
                  '14+ days of continuous observations required to establish personal envelope.',
                  style: TextStyle(color: AppTheme.textMuted, fontSize: 11),
                  textAlign: TextAlign.center,
                ),
              ],
            ),
          ),
        ),
      );
    }

    final latestVal = widget.values.last;
    final isWithinBaseline = latestVal >= widget.baselineLow && latestVal <= widget.baselineHigh;
    final diff = latestVal - widget.baselineMean;
    final sign = diff >= 0 ? '+' : '';
    final diffFormatted = '$sign${diff.toStringAsFixed(1)} ${widget.unit}';

    return _buildContainer(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // Header: Metric Name & Current Value vs Baseline Envelope
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      widget.metricName,
                      style: const TextStyle(
                        color: AppTheme.textPrimary,
                        fontSize: 14.5,
                        fontWeight: FontWeight.w800,
                        letterSpacing: -0.2,
                      ),
                    ),
                    const SizedBox(height: 2),
                    Row(
                      children: [
                        Container(
                          width: 8,
                          height: 8,
                          decoration: BoxDecoration(
                            color: widget.lineColor,
                            shape: BoxShape.circle,
                          ),
                        ),
                        const SizedBox(width: 6),
                        Expanded(
                          child: Text(
                            'Personal baseline: ${widget.baselineLow.toStringAsFixed(1)} – ${widget.baselineHigh.toStringAsFixed(1)} ${widget.unit}',
                            style: const TextStyle(color: AppTheme.textSecondary, fontSize: 11.5, fontWeight: FontWeight.w500),
                            overflow: TextOverflow.ellipsis,
                          ),
                        ),
                      ],
                    ),
                  ],
                ),
              ),
              Column(
                crossAxisAlignment: CrossAxisAlignment.end,
                children: [
                  Row(
                    crossAxisAlignment: CrossAxisAlignment.baseline,
                    textBaseline: TextBaseline.alphabetic,
                    children: [
                      Text(
                        latestVal.toStringAsFixed(1),
                        style: const TextStyle(
                          color: AppTheme.textPrimary,
                          fontSize: 20,
                          fontWeight: FontWeight.w900,
                          letterSpacing: -0.5,
                        ),
                      ),
                      const SizedBox(width: 3),
                      Text(
                        widget.unit,
                        style: const TextStyle(color: AppTheme.textMuted, fontSize: 11, fontWeight: FontWeight.w600),
                      ),
                    ],
                  ),
                  const SizedBox(height: 2),
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                    decoration: BoxDecoration(
                      color: isWithinBaseline
                          ? AppTheme.stable.withValues(alpha: 0.12)
                          : AppTheme.warning.withValues(alpha: 0.12),
                      borderRadius: BorderRadius.circular(4),
                    ),
                    child: Text(
                      isWithinBaseline ? 'Within baseline' : '$diffFormatted from mean',
                      style: TextStyle(
                        color: isWithinBaseline ? AppTheme.stable : AppTheme.warning,
                        fontSize: 10,
                        fontWeight: FontWeight.w700,
                      ),
                    ),
                  ),
                ],
              ),
            ],
          ),

          const SizedBox(height: 16),

          // Interactive Custom Chart Canvas with Gesture Detection
          SizedBox(
            height: 120,
            width: double.infinity,
            child: LayoutBuilder(
              builder: (context, constraints) {
                return GestureDetector(
                  onPanDown: (details) => _updateHover(details.localPosition.dx, constraints.maxWidth),
                  onPanUpdate: (details) => _updateHover(details.localPosition.dx, constraints.maxWidth),
                  onPanEnd: (_) => setState(() => _hoveredIndex = null),
                  child: CustomPaint(
                    size: Size(constraints.maxWidth, 120),
                    painter: _SignalChartPainter(
                      values: widget.values,
                      baselineMean: widget.baselineMean,
                      baselineLow: widget.baselineLow,
                      baselineHigh: widget.baselineHigh,
                      lineColor: widget.lineColor,
                      hoveredIndex: _hoveredIndex,
                    ),
                  ),
                );
              },
            ),
          ),

          const SizedBox(height: 8),

          // X-Axis Labels & Hover Tooltip
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(
                widget.labels.isNotEmpty ? widget.labels.first : '',
                style: const TextStyle(color: AppTheme.textMuted, fontSize: 10, fontWeight: FontWeight.w600),
              ),
              if (_hoveredIndex != null && _hoveredIndex! < widget.values.length)
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
                  decoration: BoxDecoration(
                    color: AppTheme.textPrimary,
                    borderRadius: BorderRadius.circular(4),
                  ),
                  child: Text(
                    '${widget.labels[_hoveredIndex!]}: ${widget.values[_hoveredIndex!].toStringAsFixed(1)} ${widget.unit}',
                    style: const TextStyle(color: Colors.white, fontSize: 10.5, fontWeight: FontWeight.w600),
                  ),
                )
              else
                const Flexible(
                  child: Text(
                    'Past 7 Days (Telemetry)',
                    style: TextStyle(color: AppTheme.textMuted, fontSize: 10, fontWeight: FontWeight.w500),
                    overflow: TextOverflow.ellipsis,
                  ),
                ),
              Text(
                widget.labels.isNotEmpty ? widget.labels.last : '',
                style: const TextStyle(color: AppTheme.textMuted, fontSize: 10, fontWeight: FontWeight.w600),
              ),
            ],
          ),
        ],
      ),
    );
  }

  void _updateHover(double localX, double width) {
    if (widget.values.isEmpty) return;
    final step = width / (widget.values.length - 1);
    final idx = (localX / step).round().clamp(0, widget.values.length - 1);
    setState(() => _hoveredIndex = idx);
  }

  Widget _buildContainer({required Widget child}) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: AppTheme.surface,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: AppTheme.border, width: 1),
        boxShadow: AppTheme.cardShadow,
      ),
      child: child,
    );
  }
}

class _SignalChartPainter extends CustomPainter {
  final List<double> values;
  final double baselineMean;
  final double baselineLow;
  final double baselineHigh;
  final Color lineColor;
  final int? hoveredIndex;

  _SignalChartPainter({
    required this.values,
    required this.baselineMean,
    required this.baselineLow,
    required this.baselineHigh,
    required this.lineColor,
    this.hoveredIndex,
  });

  @override
  void paint(Canvas canvas, Size size) {
    if (values.isEmpty) return;

    // Calculate Y domain range with 15% padding
    double minVal = values.reduce((a, b) => a < b ? a : b);
    double maxVal = values.reduce((a, b) => a > b ? a : b);
    minVal = [minVal, baselineLow].reduce((a, b) => a < b ? a : b);
    maxVal = [maxVal, baselineHigh].reduce((a, b) => a > b ? a : b);

    final span = (maxVal - minVal) == 0 ? 1.0 : (maxVal - minVal);
    final pad = span * 0.15;
    final yMin = minVal - pad;
    final yMax = maxVal + pad;
    final ySpan = yMax - yMin;

    double toY(double v) {
      final norm = (v - yMin) / ySpan;
      return size.height - (norm * size.height);
    }

    final stepX = size.width / (values.length - 1);

    // 1. Draw Baseline Range Band (Shaded Envelope)
    final yBandTop = toY(baselineHigh);
    final yBandBottom = toY(baselineLow);

    final bandPaint = Paint()
      ..color = lineColor.withValues(alpha: 0.08)
      ..style = PaintingStyle.fill;

    canvas.drawRect(
      Rect.fromLTRB(0, yBandTop, size.width, yBandBottom),
      bandPaint,
    );

    // Envelope bounding dotted/dashed lines
    final envelopeBorderPaint = Paint()
      ..color = lineColor.withValues(alpha: 0.3)
      ..strokeWidth = 1.0
      ..style = PaintingStyle.stroke;

    _drawDashedHorizontalLine(canvas, 0, size.width, yBandTop, envelopeBorderPaint);
    _drawDashedHorizontalLine(canvas, 0, size.width, yBandBottom, envelopeBorderPaint);

    // Mean center line (thin)
    final meanPaint = Paint()
      ..color = lineColor.withValues(alpha: 0.22)
      ..strokeWidth = 1.0
      ..style = PaintingStyle.stroke;
    _drawDashedHorizontalLine(canvas, 0, size.width, toY(baselineMean), meanPaint);

    // 2. Draw Data Curve (Smooth spline / line)
    final path = Path();
    final fillPath = Path();

    final points = <Offset>[];
    for (int i = 0; i < values.length; i++) {
      final x = i * stepX;
      final y = toY(values[i]);
      points.add(Offset(x, y));
      if (i == 0) {
        path.moveTo(x, y);
        fillPath.moveTo(x, y);
      } else {
        path.lineTo(x, y);
        fillPath.lineTo(x, y);
      }
    }

    fillPath.lineTo(size.width, size.height);
    fillPath.lineTo(0, size.height);
    fillPath.close();

    // Subtle gradient below the curve
    final fillGradient = LinearGradient(
      colors: [
        lineColor.withValues(alpha: 0.18),
        lineColor.withValues(alpha: 0.0),
      ],
      begin: Alignment.topCenter,
      end: Alignment.bottomCenter,
    );

    final fillPaint = Paint()
      ..shader = fillGradient.createShader(Rect.fromLTWH(0, 0, size.width, size.height))
      ..style = PaintingStyle.fill;
    canvas.drawPath(fillPath, fillPaint);

    // Main line stroke
    final strokePaint = Paint()
      ..color = lineColor
      ..strokeWidth = 2.4
      ..style = PaintingStyle.stroke
      ..strokeCap = StrokeCap.round
      ..strokeJoin = StrokeJoin.round;
    canvas.drawPath(path, strokePaint);

    // 3. Draw Data Points
    for (int i = 0; i < points.length; i++) {
      final pt = points[i];
      final isHovered = hoveredIndex == i;
      final isLast = i == points.length - 1;

      if (isHovered || isLast) {
        // Outer halo
        canvas.drawCircle(
          pt,
          isHovered ? 6.5 : 5.0,
          Paint()..color = lineColor.withValues(alpha: 0.25),
        );
      }

      // Inner point
      canvas.drawCircle(
        pt,
        isHovered ? 4.0 : 3.0,
        Paint()..color = Colors.white,
      );
      canvas.drawCircle(
        pt,
        isHovered ? 4.0 : 3.0,
        Paint()
          ..color = lineColor
          ..style = PaintingStyle.stroke
          ..strokeWidth = 2.0,
      );
    }
  }

  void _drawDashedHorizontalLine(Canvas canvas, double x1, double x2, double y, Paint paint) {
    const dashWidth = 4.0;
    const dashSpace = 4.0;
    double currentX = x1;
    while (currentX < x2) {
      canvas.drawLine(
        Offset(currentX, y),
        Offset((currentX + dashWidth).clamp(x1, x2), y),
        paint,
      );
      currentX += dashWidth + dashSpace;
    }
  }

  @override
  bool shouldRepaint(covariant _SignalChartPainter oldDelegate) {
    return oldDelegate.values != values ||
        oldDelegate.hoveredIndex != hoveredIndex ||
        oldDelegate.lineColor != lineColor ||
        oldDelegate.baselineLow != baselineLow ||
        oldDelegate.baselineHigh != baselineHigh;
  }
}
