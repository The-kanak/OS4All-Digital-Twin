import 'package:flutter/material.dart';
import '../../core/theme/app_theme.dart';
import '../../core/utils/demo_data.dart';
import '../../shared/widgets/healthcare_widgets.dart';
import 'report_upload_screen.dart';

class LabsScreen extends StatefulWidget {
  const LabsScreen({super.key});

  @override
  State<LabsScreen> createState() => _LabsScreenState();
}

class _LabsScreenState extends State<LabsScreen> with SingleTickerProviderStateMixin {
  late TabController _tabController;

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 2, vsync: this);
  }

  @override
  void dispose() {
    _tabController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppTheme.background,
      appBar: AppBar(
        title: const Text('Lab Vault & Tests'),
        actions: [
          IconButton(
            tooltip: 'Upload New Report',
            icon: const Icon(Icons.upload_file_rounded, color: AppTheme.primary),
            onPressed: () => Navigator.of(context).push(
              MaterialPageRoute(builder: (_) => const ReportUploadScreen()),
            ),
          ),
        ],
        bottom: TabBar(
          controller: _tabController,
          indicatorColor: AppTheme.primary,
          indicatorWeight: 3,
          labelColor: AppTheme.primary,
          unselectedLabelColor: AppTheme.textSecondary,
          labelStyle: const TextStyle(fontWeight: FontWeight.w700, fontSize: 13.5),
          tabs: [
            Tab(
              text: 'Confirmed Reports (${DemoData.labReports.length})',
              icon: const Icon(Icons.description_rounded, size: 18),
            ),
            Tab(
              text: 'Upcoming Tests (${DemoData.upcomingTests.length})',
              icon: const Icon(Icons.event_available_rounded, size: 18),
            ),
          ],
        ),
      ),
      floatingActionButton: FloatingActionButton.extended(
        backgroundColor: AppTheme.primary,
        foregroundColor: Colors.white,
        icon: const Icon(Icons.add_a_photo_outlined),
        label: const Text('Upload Report (OCR)', style: TextStyle(fontWeight: FontWeight.w700)),
        onPressed: () => Navigator.of(context).push(
          MaterialPageRoute(builder: (_) => const ReportUploadScreen()),
        ),
      ),
      body: TabBarView(
        controller: _tabController,
        children: [
          _buildConfirmedReportsTab(),
          _buildUpcomingTestsTab(),
        ],
      ),
    );
  }

  Widget _buildConfirmedReportsTab() {
    return ListView(
      padding: const EdgeInsets.fromLTRB(16, 16, 16, 80),
      children: [
        const DemoDataNoticeBanner(label: 'DEMO DATA — NOT A REAL PATIENT'),
        ...DemoData.labReports.map((rep) {
          final biomarkers = (rep['biomarkers'] as List<dynamic>);
          return Padding(
            padding: const EdgeInsets.only(bottom: 16),
            child: HealthcareCard(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Text(rep['date'], style: const TextStyle(color: AppTheme.textMuted, fontSize: 12, fontWeight: FontWeight.w600)),
                      HealthcareBadge(label: rep['status'], color: AppTheme.stable),
                    ],
                  ),
                  const SizedBox(height: 6),
                  Text(rep['title'], style: const TextStyle(color: AppTheme.textPrimary, fontSize: 16, fontWeight: FontWeight.w700)),
                  Text(rep['lab'], style: const TextStyle(color: AppTheme.primary, fontSize: 13, fontWeight: FontWeight.w600)),
                  const Divider(color: AppTheme.border, height: 24),
                  ...biomarkers.map((bm) => Padding(
                        padding: const EdgeInsets.only(bottom: 8),
                        child: Row(
                          mainAxisAlignment: MainAxisAlignment.spaceBetween,
                          children: [
                            Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Text(bm['name'], style: const TextStyle(color: AppTheme.textPrimary, fontSize: 13.5, fontWeight: FontWeight.w600)),
                                Text('Ref: ${bm['ref']}', style: const TextStyle(color: AppTheme.textMuted, fontSize: 11)),
                              ],
                            ),
                            Text('${bm['value']} ${bm['unit']}', style: const TextStyle(color: AppTheme.primary, fontSize: 14, fontWeight: FontWeight.w700)),
                          ],
                        ),
                      )),
                ],
              ),
            ),
          );
        }),
      ],
    );
  }

  Widget _buildUpcomingTestsTab() {
    return ListView(
      padding: const EdgeInsets.fromLTRB(16, 16, 16, 80),
      children: [
        const DemoDataNoticeBanner(label: 'DEMO DATA — NOT A REAL PATIENT'),
        ...DemoData.upcomingTests.map((test) {
          return Padding(
            padding: const EdgeInsets.only(bottom: 16),
            child: HealthcareCard(
            borderColor: AppTheme.primary.withValues(alpha: 0.35),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Row(
                      children: [
                        Container(
                          padding: const EdgeInsets.all(8),
                          decoration: BoxDecoration(
                            color: AppTheme.primary.withValues(alpha: 0.12),
                            borderRadius: BorderRadius.circular(10),
                          ),
                          child: Icon(
                            test['icon'] == 'biotech'
                                ? Icons.biotech_rounded
                                : test['icon'] == 'favorite'
                                    ? Icons.favorite_rounded
                                    : Icons.water_drop_rounded,
                            color: AppTheme.primary,
                            size: 20,
                          ),
                        ),
                        const SizedBox(width: 10),
                        Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              test['category'],
                              style: const TextStyle(color: AppTheme.primary, fontSize: 11, fontWeight: FontWeight.bold),
                            ),
                            Text(
                              test['daysLeft'],
                              style: const TextStyle(color: AppTheme.textMuted, fontSize: 11, fontWeight: FontWeight.w600),
                            ),
                          ],
                        ),
                      ],
                    ),
                    HealthcareBadge(
                      label: test['status'],
                      color: test['status'] == 'SCHEDULED' ? AppTheme.primary : AppTheme.secondary,
                    ),
                  ],
                ),
                const SizedBox(height: 12),
                Text(
                  test['title'],
                  style: const TextStyle(color: AppTheme.textPrimary, fontSize: 15.5, fontWeight: FontWeight.w800),
                ),
                const SizedBox(height: 4),
                Row(
                  children: [
                    const Icon(Icons.location_on_outlined, color: AppTheme.textMuted, size: 14),
                    const SizedBox(width: 4),
                    Text(test['laboratory'], style: const TextStyle(color: AppTheme.textSecondary, fontSize: 12)),
                  ],
                ),
                const SizedBox(height: 12),
                Container(
                  padding: const EdgeInsets.all(12),
                  decoration: BoxDecoration(
                    color: AppTheme.surfaceElevated,
                    borderRadius: BorderRadius.circular(12),
                    border: Border.all(color: AppTheme.border),
                  ),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Row(
                        children: [
                          const Icon(Icons.info_outline_rounded, size: 14, color: AppTheme.primary),
                          const SizedBox(width: 6),
                          Expanded(
                            child: Text(
                              'Reason: ${test['reason']}',
                              style: const TextStyle(color: AppTheme.textPrimary, fontSize: 12, fontWeight: FontWeight.w600),
                            ),
                          ),
                        ],
                      ),
                      const SizedBox(height: 6),
                      Row(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          const Icon(Icons.timer_outlined, size: 14, color: AppTheme.textSecondary),
                          const SizedBox(width: 6),
                          Expanded(
                            child: Text(
                              'Prep: ${test['preparation']}',
                              style: const TextStyle(color: AppTheme.textSecondary, fontSize: 11.5),
                            ),
                          ),
                        ],
                      ),
                    ],
                  ),
                ),
                const SizedBox(height: 12),
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Row(
                      children: [
                        const Icon(Icons.calendar_month_rounded, size: 15, color: AppTheme.primary),
                        const SizedBox(width: 6),
                        Text(
                          'Scheduled: ${test['scheduledDate']}',
                          style: const TextStyle(color: AppTheme.textPrimary, fontSize: 12.5, fontWeight: FontWeight.bold),
                        ),
                      ],
                    ),
                    TextButton.icon(
                      style: TextButton.styleFrom(
                        visualDensity: VisualDensity.compact,
                        padding: const EdgeInsets.symmetric(horizontal: 8),
                      ),
                      icon: const Icon(Icons.notifications_active_outlined, size: 14, color: AppTheme.primary),
                      label: const Text('Remind Me', style: TextStyle(color: AppTheme.primary, fontSize: 11.5, fontWeight: FontWeight.bold)),
                      onPressed: () {
                        ScaffoldMessenger.of(context).showSnackBar(
                          SnackBar(
                            content: Text('Reminder set for ${test['title']} on ${test['scheduledDate']}'),
                            backgroundColor: AppTheme.primary,
                            behavior: SnackBarBehavior.floating,
                          ),
                        );
                      },
                    ),
                  ],
                ),
              ],
            ),
          ),
        );
      }),
    ],
  );
}
}
