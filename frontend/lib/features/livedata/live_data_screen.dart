import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:intl/intl.dart';
import '../../core/theme/mobile_design_system.dart';
import '../../shared/widgets/os4_components.dart';
import '../digitaltwin/digital_twin_controller.dart';
import '../healthconnect/health_connect_model.dart';

/// SCREEN 3 — LIVE
/// "Where is the data coming from?"
/// Transparently displays data source status, physical Android Health Connect integration,
/// wearable connectivity disclosures, and honest competition simulation modes.
class LiveDataScreen extends StatelessWidget {
  const LiveDataScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final controller = DigitalTwinController();

    return ListenableBuilder(
      listenable: controller,
      builder: (context, _) {
        final telem = controller.latestTelemetry ?? {};
        final hcStatus = controller.healthConnectStatus;
        final hcSnapshot = controller.healthConnectSnapshot;

        return Scaffold(
          backgroundColor: MobileTheme.background,
          body: SafeArea(
            child: SingleChildScrollView(
              padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  // 1. Header
                  _buildHeader(),
                  const SizedBox(height: 12),

                  // 2. Data Source Status Banner
                  _buildSourceStatusBanner(hcStatus),
                  const SizedBox(height: 14),

                  // 3. Android Health Connect Dedicated Card
                  _buildHealthConnectCard(context, controller, hcStatus, hcSnapshot),
                  const SizedBox(height: 16),

                  // 4. Real-Time Telemetry Grid
                  _buildRealtimeSignalsGrid(telem, controller),
                  const SizedBox(height: 18),

                  // 5. Wearables & Devices Architecture
                  _buildWearablesAndDevicesSection(hcStatus),
                  const SizedBox(height: 18),

                  // 6. Simulation Mode (Competition Demo Data)
                  _buildSimulationModeSection(controller),
                  const SizedBox(height: 16),

                  // Bottom Disclaimer
                  const PrototypeDisclaimerBanner(),
                  const SizedBox(height: 20),
                ],
              ),
            ),
          ),
        );
      },
    );
  }

  Widget _buildHeader() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          'Live Signals',
          style: GoogleFonts.inter(
            fontSize: 22,
            fontWeight: FontWeight.w800,
            color: MobileTheme.textPrimary,
            letterSpacing: -0.4,
          ),
        ),
        const SizedBox(height: 2),
        Text(
          'Where is the data coming from? Ingestion streams & device pairings.',
          style: GoogleFonts.inter(fontSize: 12, color: MobileTheme.textSecondary),
        ),
      ],
    );
  }

  Widget _buildSourceStatusBanner(HealthConnectStatus hcStatus) {
    final isPhysical = hcStatus == HealthConnectStatus.PERMISSION_GRANTED;

    return Container(
      width: double.infinity,
      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
      decoration: BoxDecoration(
        color: MobileTheme.surface,
        borderRadius: MobileTheme.cardRadius,
        border: Border.all(
          color: isPhysical ? MobileTheme.stable.withValues(alpha: 0.5) : MobileTheme.drift.withValues(alpha: 0.5),
          width: 1,
        ),
      ),
      child: Row(
        children: [
          Container(
            width: 38,
            height: 38,
            decoration: BoxDecoration(
              color: isPhysical ? MobileTheme.stable.withValues(alpha: 0.15) : MobileTheme.drift.withValues(alpha: 0.15),
              borderRadius: BorderRadius.circular(10),
            ),
            child: Icon(
              isPhysical ? Icons.smartphone_rounded : Icons.hub_rounded,
              color: isPhysical ? MobileTheme.stable : MobileTheme.drift,
              size: 20,
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
                  spacing: 4,
                  children: [
                    Text(
                      'DATA SOURCE: ',
                      style: GoogleFonts.inter(
                        fontSize: 10,
                        fontWeight: FontWeight.w800,
                        color: MobileTheme.textSecondary,
                        letterSpacing: 0.5,
                      ),
                    ),
                    Text(
                      isPhysical ? 'PHYSICAL DEVICE' : 'SIMULATION MODE',
                      style: GoogleFonts.inter(
                        fontSize: 11,
                        fontWeight: FontWeight.w800,
                        color: isPhysical ? MobileTheme.stable : MobileTheme.drift,
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 2),
                Text(
                  isPhysical
                      ? 'Android Health Connect native records active'
                      : 'Synthetic Twin Telemetry (5-sec streaming)',
                  style: GoogleFonts.inter(fontSize: 11.5, color: MobileTheme.textPrimary),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildHealthConnectCard(
    BuildContext context,
    DigitalTwinController controller,
    HealthConnectStatus status,
    HealthConnectSnapshot snapshot,
  ) {
    final isConnected = status == HealthConnectStatus.PERMISSION_GRANTED;
    final isUnavailable = status == HealthConnectStatus.HEALTH_CONNECT_UNAVAILABLE;

    String statusText = 'Permission Required';
    Color statusColor = MobileTheme.drift;
    if (isConnected) {
      statusText = 'Connected';
      statusColor = MobileTheme.stable;
    } else if (isUnavailable) {
      statusText = 'Not Available';
      statusColor = MobileTheme.textSubtle;
    }

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
                  const Icon(Icons.favorite_rounded, color: MobileTheme.critical, size: 18),
                  const SizedBox(width: 8),
                  Flexible(
                    child: Text(
                      'Android Health Connect',
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: GoogleFonts.inter(
                        fontSize: 14,
                        fontWeight: FontWeight.w700,
                        color: MobileTheme.textPrimary,
                      ),
                    ),
                  ),
                ],
              ),
              StatusPill(label: statusText, color: statusColor),
            ],
          ),
          const SizedBox(height: 6),
          Text(
            'Physical on-device biometric store via Android Health Platform.',
            style: GoogleFonts.inter(fontSize: 11.5, color: MobileTheme.textSecondary),
          ),
          const SizedBox(height: 14),

          // Supported Metrics List
          Container(
            padding: const EdgeInsets.all(12),
            decoration: BoxDecoration(
              color: MobileTheme.surfaceElevated,
              borderRadius: BorderRadius.circular(12),
              border: Border.all(color: MobileTheme.border),
            ),
            child: Column(
              children: [
                _buildHcMetricRow(
                  'Blood Glucose',
                  snapshot.bloodGlucoseMgDl != null
                      ? '${snapshot.bloodGlucoseMgDl!.toStringAsFixed(0)} mg/dL'
                      : (isConnected ? 'Mapped (mg/dL)' : 'Permission pending'),
                  Icons.water_drop_outlined,
                ),
                _buildHcMetricRow(
                  'Heart Rate',
                  snapshot.heartRateBpm != null
                      ? '${snapshot.heartRateBpm!.toInt()} BPM'
                      : (snapshot.restingHeartRateBpm != null
                          ? '${snapshot.restingHeartRateBpm!.toInt()} BPM'
                          : (isConnected ? 'Continuous (BPM)' : 'Permission pending')),
                  Icons.favorite_border_rounded,
                ),
                _buildHcMetricRow(
                  'Heart Rate Variability',
                  snapshot.hrvMs != null
                      ? '${snapshot.hrvMs!.toStringAsFixed(0)} ms'
                      : (isConnected ? 'RMSSD (ms)' : 'Permission pending'),
                  Icons.monitor_heart_outlined,
                ),
                _buildHcMetricRow(
                  'Steps & Cadence',
                  snapshot.steps != null
                      ? '${snapshot.steps} steps'
                      : (isConnected ? 'Daily tally' : 'Permission pending'),
                  Icons.directions_walk_rounded,
                ),
                _buildHcMetricRow(
                  'Sleep Session',
                  snapshot.sleepDurationHours != null
                      ? '${snapshot.sleepDurationHours!.toStringAsFixed(1)} hrs'
                      : (isConnected ? 'Duration & stages' : 'Permission pending'),
                  Icons.bedtime_outlined,
                ),
              ],
            ),
          ),
          const SizedBox(height: 14),

          // Action Button
          PrimaryButton(
            label: isConnected ? 'Sync Health Connect Now' : 'Connect Android Health Platform',
            icon: Icons.sync_rounded,
            isLoading: controller.isLoading,
            onPressed: () async {
              await controller.connectHealthConnect();
              if (context.mounted) {
                _showHealthConnectSyncFeedback(context, controller);
              }
            },
          ),
          const SizedBox(height: 8),
          Center(
            child: Text(
              controller.lastHealthConnectSyncTime != null
                  ? (DateTime.now().difference(controller.lastHealthConnectSyncTime!).inMinutes < 1
                      ? 'Last synced: Just now'
                      : 'Last synced at ${DateFormat('h:mm a').format(controller.lastHealthConnectSyncTime!)}')
                  : (isConnected ? 'Connected • Ready to sync' : 'Health Connect is unavailable or permissions are not granted.'),
              style: GoogleFonts.inter(fontSize: 10.5, color: MobileTheme.textSubtle),
            ),
          ),
        ],
      ),
    );
  }

  void _showHealthConnectSyncFeedback(BuildContext context, DigitalTwinController controller) {
    final status = controller.healthConnectStatus;
    final snapshot = controller.healthConnectSnapshot;

    String title;
    String? subtitle;
    IconData icon;
    Color iconColor;

    if (status == HealthConnectStatus.PERMISSION_GRANTED) {
      if (snapshot.hasAnyHealthData) {
        // Real count of non-null metrics returned from the physical sensor bridge
        int count = 0;
        if (snapshot.bloodGlucoseMgDl != null) count++;
        if (snapshot.restingHeartRateBpm != null || snapshot.heartRateBpm != null) count++;
        if (snapshot.hrvMs != null) count++;
        if (snapshot.steps != null) count++;
        if (snapshot.sleepDurationHours != null) count++;

        title = 'Health Connect synced successfully';
        if (count > 0) {
          subtitle = '$count health signal${count > 1 ? 's' : ''} updated';
        }
        icon = Icons.check_circle_rounded;
        iconColor = const Color(0xFF22C55E); // Green (#22C55E)
      } else {
        title = 'Health Connect connected';
        subtitle = 'No recent records found on device';
        icon = Icons.info_outline_rounded;
        iconColor = const Color(0xFFF59E0B); // Amber (#F59E0B)
      }
    } else if (status == HealthConnectStatus.PERMISSION_NOT_GRANTED) {
      title = 'Health Connect permission required';
      subtitle = 'Grant permissions in Android Settings to sync';
      icon = Icons.security_rounded;
      iconColor = const Color(0xFFF59E0B); // Amber (#F59E0B)
    } else {
      title = 'Health Connect sync failed';
      subtitle = (snapshot.errorMessage != null && snapshot.errorMessage!.isNotEmpty)
          ? snapshot.errorMessage!
          : 'Try again or check Health Connect availability';
      icon = Icons.error_outline_rounded;
      iconColor = const Color(0xFFEF4444); // Coral Red (#EF4444)
    }

    ScaffoldMessenger.of(context).clearSnackBars();
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        behavior: SnackBarBehavior.floating,
        margin: const EdgeInsets.only(left: 16, right: 16, bottom: 16),
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
        duration: const Duration(seconds: 3),
        elevation: 6,
        backgroundColor: const Color(0xFF0F172A), // Dark navy semantic success surface
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(12),
          side: const BorderSide(color: Color(0xFF334155), width: 1),
        ),
        content: Row(
          crossAxisAlignment: CrossAxisAlignment.center,
          children: [
            Container(
              width: 34,
              height: 34,
              decoration: BoxDecoration(
                color: iconColor.withValues(alpha: 0.15),
                shape: BoxShape.circle,
              ),
              child: Icon(icon, color: iconColor, size: 20),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    title,
                    style: GoogleFonts.inter(
                      fontSize: 13.5,
                      fontWeight: FontWeight.w700,
                      color: Colors.white,
                      letterSpacing: -0.2,
                    ),
                  ),
                  if (subtitle != null) ...[
                    const SizedBox(height: 2),
                    Text(
                      subtitle,
                      style: GoogleFonts.inter(
                        fontSize: 11.5,
                        fontWeight: FontWeight.w400,
                        color: const Color(0xFFCBD5E1),
                      ),
                    ),
                  ],
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildHcMetricRow(String title, String status, IconData icon) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Expanded(
            child: Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                Icon(icon, size: 14, color: MobileTheme.primary),
                const SizedBox(width: 8),
                Flexible(
                  child: Text(
                    title,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: GoogleFonts.inter(fontSize: 11.5, color: MobileTheme.textPrimary),
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(width: 8),
          Text(
            status,
            style: GoogleFonts.inter(fontSize: 10.5, color: MobileTheme.textSecondary, fontWeight: FontWeight.w500),
          ),
        ],
      ),
    );
  }

  Widget _buildRealtimeSignalsGrid(Map<String, dynamic> telem, DigitalTwinController controller) {
    final glucose = (telem['glucose'] as num?)?.toDouble() ?? controller.currentGlucose;
    final hr = (telem['restingHeartRate'] as num?)?.toInt() ?? 78;
    final hrv = (telem['heartRateVariability'] as num?)?.toDouble() ?? 42.0;
    final sleep = (telem['sleepHours'] as num?)?.toDouble() ?? 5.8;
    final steps = (telem['dailySteps'] as num?)?.toInt() ?? 2850;
    final activity = telem['activityLevel']?.toString() ?? 'Sedentary';

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          'REAL-TIME SIGNALS',
          style: GoogleFonts.inter(
            fontSize: 12,
            fontWeight: FontWeight.w800,
            color: MobileTheme.textPrimary,
            letterSpacing: 0.5,
          ),
        ),
        const SizedBox(height: 8),
        LayoutBuilder(
          builder: (context, constraints) {
            final cardWidth = (constraints.maxWidth - 10) / 2;
            return Wrap(
              spacing: 10,
              runSpacing: 10,
              children: [
                SizedBox(
                  width: cardWidth,
                  child: SignalCard(
                    label: 'CGM Glucose',
                    value: glucose.toStringAsFixed(0),
                    unit: 'mg/dL',
                    icon: Icons.water_drop_rounded,
                    color: MobileTheme.primary,
                    subtitle: 'Continuous stream',
                  ),
                ),
                SizedBox(
                  width: cardWidth,
                  child: SignalCard(
                    label: 'Heart Rate',
                    value: '$hr',
                    unit: 'BPM',
                    icon: Icons.favorite_rounded,
                    color: MobileTheme.critical,
                    subtitle: 'Photoplethysmography',
                  ),
                ),
                SizedBox(
                  width: cardWidth,
                  child: SignalCard(
                    label: 'Nocturnal HRV',
                    value: hrv.toStringAsFixed(0),
                    unit: 'ms',
                    icon: Icons.monitor_heart_rounded,
                    color: MobileTheme.secondaryAccent,
                    subtitle: 'Vagal autonomic index',
                  ),
                ),
                SizedBox(
                  width: cardWidth,
                  child: SignalCard(
                    label: 'Sleep Duration',
                    value: sleep.toStringAsFixed(1),
                    unit: 'hrs',
                    icon: Icons.bedtime_rounded,
                    color: MobileTheme.primary,
                    subtitle: 'Last night recovery',
                  ),
                ),
                SizedBox(
                  width: cardWidth,
                  child: SignalCard(
                    label: 'Step Ingestion',
                    value: '$steps',
                    unit: 'steps',
                    icon: Icons.directions_walk_rounded,
                    color: MobileTheme.stable,
                    subtitle: 'Pedometry log',
                  ),
                ),
                SizedBox(
                  width: cardWidth,
                  child: SignalCard(
                    label: 'Postural State',
                    value: activity.split(' ').first,
                    unit: '',
                    icon: Icons.bolt_rounded,
                    color: MobileTheme.drift,
                    subtitle: 'Kinematic tracking',
                  ),
                ),
              ],
            );
          },
        ),
      ],
    );
  }

  Widget _buildWearablesAndDevicesSection(HealthConnectStatus hcStatus) {
    final isHcConnected = hcStatus == HealthConnectStatus.PERMISSION_GRANTED;

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          'WEARABLES & DEVICES',
          style: GoogleFonts.inter(
            fontSize: 12,
            fontWeight: FontWeight.w800,
            color: MobileTheme.textPrimary,
            letterSpacing: 0.5,
          ),
        ),
        const SizedBox(height: 8),
        Container(
          width: double.infinity,
          decoration: BoxDecoration(
            color: MobileTheme.surface,
            borderRadius: MobileTheme.cardRadius,
            border: Border.all(color: MobileTheme.border),
          ),
          child: Column(
            children: [
              _buildDeviceTile(
                title: 'Health Connect',
                subtitle: 'Android Health Platform Bridge',
                status: isHcConnected ? 'Connected' : 'Available',
                color: isHcConnected ? MobileTheme.stable : MobileTheme.primary,
                icon: Icons.smartphone_rounded,
              ),
              const Divider(color: MobileTheme.border, height: 1),
              _buildDeviceTile(
                title: 'Smartwatch / Band',
                subtitle: 'Continuous BLE telemetry bridge',
                status: 'Not connected',
                color: MobileTheme.textSubtle,
                icon: Icons.watch_rounded,
              ),
              const Divider(color: MobileTheme.border, height: 1),
              _buildDeviceTile(
                title: 'Continuous Glucose Monitor (CGM)',
                subtitle: 'Direct sensor NFC / BLE ingestion',
                status: 'Not connected',
                color: MobileTheme.textSubtle,
                icon: Icons.bloodtype_outlined,
              ),
              const Divider(color: MobileTheme.border, height: 1),
              _buildDeviceTile(
                title: 'Fitness Tracker',
                subtitle: 'Step cadence & caloric expenditure',
                status: 'Not connected',
                color: MobileTheme.textSubtle,
                icon: Icons.fitness_center_rounded,
              ),
            ],
          ),
        ),
      ],
    );
  }

  Widget _buildDeviceTile({
    required String title,
    required String subtitle,
    required String status,
    required Color color,
    required IconData icon,
  }) {
    return Material(
      color: Colors.transparent,
      child: ListTile(
        contentPadding: const EdgeInsets.symmetric(horizontal: 14, vertical: 2),
        leading: Container(
          width: 36,
          height: 36,
          decoration: BoxDecoration(
            color: color.withValues(alpha: 0.12),
            borderRadius: BorderRadius.circular(10),
          ),
          child: Icon(icon, color: color, size: 20),
        ),
        title: Text(
          title,
          maxLines: 1,
          overflow: TextOverflow.ellipsis,
          style: GoogleFonts.inter(fontSize: 13.5, fontWeight: FontWeight.w700, color: MobileTheme.textPrimary),
        ),
        subtitle: Text(
          subtitle,
          maxLines: 1,
          overflow: TextOverflow.ellipsis,
          style: GoogleFonts.inter(fontSize: 11, color: MobileTheme.textSecondary),
        ),
        trailing: StatusPill(label: status, color: color),
      ),
    );
  }

  Widget _buildSimulationModeSection(DigitalTwinController controller) {
    final isStreaming = controller.isLiveStreaming;
    final nowStr = DateFormat('HH:mm:ss').format(DateTime.now());

    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: MobileTheme.surface,
        borderRadius: MobileTheme.cardRadius,
        border: Border.all(color: MobileTheme.border),
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
                'DEMO DATA SIMULATION',
                style: GoogleFonts.inter(
                  fontSize: 11.5,
                  fontWeight: FontWeight.w800,
                  color: MobileTheme.textPrimary,
                  letterSpacing: 0.5,
                ),
              ),
              StatusPill(
                label: 'SYNTHETIC TELEMETRY',
                color: isStreaming ? MobileTheme.drift : MobileTheme.textSubtle,
              ),
            ],
          ),
          const SizedBox(height: 6),
          Text(
            'High-frequency synthetic IoT generator for live hackathon demonstration.',
            style: GoogleFonts.inter(fontSize: 11.5, color: MobileTheme.textSecondary),
          ),
          const SizedBox(height: 12),
          Wrap(
            alignment: WrapAlignment.spaceBetween,
            crossAxisAlignment: WrapCrossAlignment.center,
            spacing: 8,
            runSpacing: 4,
            children: [
              Text(
                'Update Rate: 5-second intervals',
                style: GoogleFonts.inter(fontSize: 11, color: MobileTheme.textSecondary),
              ),
              Text(
                'Timestamp: $nowStr',
                style: GoogleFonts.inter(fontSize: 11, color: MobileTheme.primary, fontWeight: FontWeight.w600),
              ),
            ],
          ),
          const SizedBox(height: 14),
          SecondaryButton(
            label: isStreaming ? 'Pause Synthetic Simulation' : 'Start Live Simulation',
            icon: isStreaming ? Icons.pause_circle_outline_rounded : Icons.play_circle_outline_rounded,
            onPressed: () => controller.toggleLiveStreaming(),
          ),
        ],
      ),
    );
  }
}
