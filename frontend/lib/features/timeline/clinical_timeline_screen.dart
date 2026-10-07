import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import '../../core/theme/mobile_design_system.dart';
import '../../shared/widgets/os4_components.dart';
import '../digitaltwin/digital_twin_controller.dart';

/// Clinical Timeline Screen
/// Longitudinal disease progression and physiological event chronology.
/// Clean vertical timeline with category filters and dark healthcare-tech styling.
class ClinicalTimelineScreen extends StatefulWidget {
  const ClinicalTimelineScreen({super.key});

  @override
  State<ClinicalTimelineScreen> createState() => _ClinicalTimelineScreenState();
}

class _ClinicalTimelineScreenState extends State<ClinicalTimelineScreen> {
  String _activeFilter = 'ALL';

  @override
  Widget build(BuildContext context) {
    final controller = DigitalTwinController();

    return ListenableBuilder(
      listenable: controller,
      builder: (context, _) {
        final rawEvents = controller.timelineEvents;
        final events = rawEvents.map((e) {
          final m = e as Map<String, dynamic>;
          return {
            'title': m['title']?.toString() ?? 'Clinical Event',
            'time': m['formattedTime']?.toString() ?? 'Recorded',
            'subtitle': m['subtitle']?.toString() ?? '',
            'category': m['category']?.toString() ?? 'SYSTEM',
          };
        }).where((e) {
          if (_activeFilter == 'ALL') return true;
          return e['category'] == _activeFilter;
        }).toList();

        return Scaffold(
          backgroundColor: MobileTheme.background,
          appBar: AppBar(
            backgroundColor: MobileTheme.surface,
            elevation: 0,
            leading: IconButton(
              icon: const Icon(Icons.arrow_back_rounded, color: MobileTheme.textPrimary),
              onPressed: () => Navigator.of(context).pop(),
            ),
            title: Text(
              'Clinical Timeline',
              style: GoogleFonts.inter(
                color: MobileTheme.textPrimary,
                fontSize: 17,
                fontWeight: FontWeight.w700,
              ),
            ),
          ),
          body: SafeArea(
            child: Column(
              children: [
                // Filter Chips Row
                Container(
                  color: MobileTheme.surface,
                  padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                  child: SingleChildScrollView(
                    scrollDirection: Axis.horizontal,
                    child: Row(
                      children: ['ALL', 'SYSTEM', 'VITALS', 'LABS'].map((f) {
                        final isSel = _activeFilter == f;
                        return Padding(
                          padding: const EdgeInsets.only(right: 8),
                          child: FilterChip(
                            label: Text(f),
                            selected: isSel,
                            onSelected: (_) => setState(() => _activeFilter = f),
                            backgroundColor: MobileTheme.surfaceElevated,
                            selectedColor: MobileTheme.primaryBg,
                            labelStyle: GoogleFonts.inter(
                              color: isSel ? MobileTheme.primary : MobileTheme.textSecondary,
                              fontWeight: isSel ? FontWeight.w700 : FontWeight.w500,
                              fontSize: 12,
                            ),
                            side: BorderSide(
                              color: isSel ? MobileTheme.primary : MobileTheme.border,
                            ),
                          ),
                        );
                      }).toList(),
                    ),
                  ),
                ),

                const Padding(
                  padding: EdgeInsets.symmetric(horizontal: 16, vertical: 10),
                  child: PrototypeDisclaimerBanner(),
                ),

                // Vertical Timeline
                Expanded(
                  child: events.isEmpty
                    ? Center(
                        child: Text(
                          'No events recorded for this category.',
                          style: GoogleFonts.inter(color: MobileTheme.textSecondary),
                        ),
                      )
                    : ListView.builder(
                        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                        itemCount: events.length,
                        itemBuilder: (context, idx) {
                          final item = events[idx];
                          final isLast = idx == events.length - 1;
                          final cat = item['category']!;

                          Color catColor = MobileTheme.primary;
                          IconData catIcon = Icons.info_outline_rounded;
                          if (cat == 'SYSTEM') {
                            catColor = MobileTheme.secondaryAccent;
                            catIcon = Icons.hub_rounded;
                          } else if (cat == 'VITALS') {
                            catColor = MobileTheme.critical;
                            catIcon = Icons.favorite_rounded;
                          } else if (cat == 'LABS') {
                            catColor = MobileTheme.drift;
                            catIcon = Icons.science_rounded;
                          }

                          return Row(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              // Timeline Node & Line
                              Column(
                                children: [
                                  Container(
                                    width: 32,
                                    height: 32,
                                    decoration: BoxDecoration(
                                      color: catColor.withOpacity(0.15),
                                      shape: BoxShape.circle,
                                      border: Border.all(color: catColor, width: 1.5),
                                    ),
                                    child: Icon(catIcon, color: catColor, size: 16),
                                  ),
                                  if (!isLast)
                                    Container(
                                      width: 2,
                                      height: 60,
                                      color: MobileTheme.border,
                                    ),
                                ],
                              ),
                              const SizedBox(width: 12),
                              // Content Card
                              Expanded(
                                child: Container(
                                  margin: const EdgeInsets.only(bottom: 14),
                                  padding: const EdgeInsets.all(12),
                                  decoration: BoxDecoration(
                                    color: MobileTheme.surface,
                                    borderRadius: BorderRadius.circular(12),
                                    border: Border.all(color: MobileTheme.border),
                                  ),
                                  child: Column(
                                    crossAxisAlignment: CrossAxisAlignment.start,
                                    children: [
                                      Row(
                                        mainAxisAlignment: MainAxisAlignment.spaceBetween,
                                        children: [
                                          StatusPill(label: cat, color: catColor),
                                          Text(
                                            item['time']!,
                                            style: GoogleFonts.inter(
                                              fontSize: 11,
                                              color: MobileTheme.textSecondary,
                                            ),
                                          ),
                                        ],
                                      ),
                                      const SizedBox(height: 6),
                                      Text(
                                        item['title']!,
                                        style: GoogleFonts.inter(
                                          fontSize: 13.5,
                                          fontWeight: FontWeight.w700,
                                          color: MobileTheme.textPrimary,
                                        ),
                                      ),
                                      if (item['subtitle']!.isNotEmpty) ...[
                                        const SizedBox(height: 3),
                                        Text(
                                          item['subtitle']!,
                                          style: GoogleFonts.inter(
                                            fontSize: 11.5,
                                            color: MobileTheme.textSecondary,
                                          ),
                                        ),
                                      ],
                                    ],
                                  ),
                                ),
                              ),
                            ],
                          );
                        },
                      ),
                ),
              ],
            ),
          ),
        );
      },
    );
  }
}
