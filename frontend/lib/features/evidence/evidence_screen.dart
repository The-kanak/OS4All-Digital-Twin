import 'package:flutter/material.dart';
import '../../core/theme/app_theme.dart';
import '../../core/utils/demo_data.dart';
import '../../shared/widgets/healthcare_widgets.dart';

class EvidenceScreen extends StatefulWidget {
  const EvidenceScreen({super.key});

  @override
  State<EvidenceScreen> createState() => _EvidenceScreenState();
}

class _EvidenceScreenState extends State<EvidenceScreen> {
  final _searchController = TextEditingController(text: 'heart rate variability autonomic recovery');
  bool _isSearching = false;

  @override
  Widget build(BuildContext context) {
    final evidenceList = (DemoData.latestAiInsight['evidence'] as List<dynamic>);

    return Scaffold(
      backgroundColor: AppTheme.background,
      appBar: AppBar(title: const Text('Biomedical Evidence (Tavily)')),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(18),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const DemoDataNoticeBanner(label: 'DEMO DATA — NOT A REAL PATIENT'),
            const Text(
              'Peer-Reviewed Scientific Literature',
              style: TextStyle(color: AppTheme.textPrimary, fontSize: 22, fontWeight: FontWeight.w800, letterSpacing: -0.5),
            ),
            const SizedBox(height: 6),
            const Text(
              'External evidence is retrieved strictly when physiological deviations are present. Searches are restricted to authoritative medical domains (PubMed, NIH, Nature Medicine).',
              style: TextStyle(color: AppTheme.textSecondary, fontSize: 13, height: 1.4),
            ),
            const SizedBox(height: 20),
            TextField(
              controller: _searchController,
              decoration: InputDecoration(
                hintText: 'Search peer-reviewed literature...',
                prefixIcon: const Icon(Icons.search, color: AppTheme.textSecondary),
                suffixIcon: IconButton(
                  icon: const Icon(Icons.arrow_forward, color: AppTheme.cyan),
                  onPressed: () {
                    setState(() => _isSearching = true);
                    Future.delayed(const Duration(milliseconds: 600), () {
                      if (mounted) setState(() => _isSearching = false);
                    });
                  },
                ),
              ),
            ),
            const SizedBox(height: 20),
            if (_isSearching)
              const HealthcareLoadingIndicator(message: 'Querying Tavily biomedical search...')
            else ...[
              const Text('Grounded Citations for Current Signals', style: TextStyle(color: AppTheme.textPrimary, fontSize: 16, fontWeight: FontWeight.w700)),
              const SizedBox(height: 12),
              ...evidenceList.map((e) => Padding(
                    padding: const EdgeInsets.only(bottom: 14),
                    child: HealthcareCard(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          const Row(
                            mainAxisAlignment: MainAxisAlignment.spaceBetween,
                            children: [
                              HealthcareBadge(label: 'PEER-REVIEWED', color: AppTheme.success),
                              Icon(Icons.open_in_new, color: AppTheme.cyan, size: 16),
                            ],
                          ),
                          const SizedBox(height: 10),
                          Text(e['title'], style: const TextStyle(color: AppTheme.textPrimary, fontSize: 15, fontWeight: FontWeight.w700)),
                          const SizedBox(height: 4),
                          Text(e['source'], style: const TextStyle(color: AppTheme.cyanMuted, fontSize: 12, fontWeight: FontWeight.w600)),
                          const SizedBox(height: 8),
                          Text(e['relevance'], style: const TextStyle(color: AppTheme.textSecondary, fontSize: 13, height: 1.35)),
                          const SizedBox(height: 10),
                          Text(e['url'], style: const TextStyle(color: AppTheme.textMuted, fontSize: 11)),
                        ],
                      ),
                    ),
                  )),
            ],
          ],
        ),
      ),
    );
  }
}
