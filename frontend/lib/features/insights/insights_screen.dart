import 'package:flutter/material.dart';
import '../../core/theme/app_theme.dart';
import '../../core/utils/demo_data.dart';
import '../../shared/widgets/healthcare_widgets.dart';
import '../evidence/evidence_screen.dart';
import './intelligence_trace_screen.dart';

class InsightsScreen extends StatelessWidget {
  const InsightsScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final insight = DemoData.latestAiInsight;
    final observations = (insight['observations'] as List<dynamic>);
    final hypotheses = (insight['possibleInterpretations'] as List<dynamic>);
    final actions = (insight['recommendedActions'] as List<dynamic>);

    return Scaffold(
      backgroundColor: AppTheme.background,
      appBar: AppBar(
        title: const Text('AI Health Intelligence'),
        actions: [
          Padding(
            padding: const EdgeInsets.only(right: 8),
            child: TextButton.icon(
              style: TextButton.styleFrom(
                backgroundColor: AppTheme.primary.withValues(alpha: 0.12),
                foregroundColor: AppTheme.primary,
                padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
              ),
              icon: const Icon(Icons.account_tree_rounded, size: 15),
              label: const Text('TRACE PIPELINE', style: TextStyle(fontSize: 11, fontWeight: FontWeight.bold)),
              onPressed: () => Navigator.of(context).push(
                MaterialPageRoute(builder: (_) => const IntelligenceTraceScreen()),
              ),
            ),
          ),
        ],
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(18),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const DemoDataNoticeBanner(label: 'DEMO DATA — NOT A REAL PATIENT'),
            HealthcareCard(
              borderColor: AppTheme.indigo.withValues(alpha: 0.5),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      const Text('Multi-Agent Synthesis', style: TextStyle(color: AppTheme.textSecondary, fontSize: 13, fontWeight: FontWeight.w600)),
                      HealthcareBadge(label: insight['urgency'], color: AppTheme.info),
                    ],
                  ),
                  const SizedBox(height: 10),
                  Text(
                    insight['summary'],
                    style: const TextStyle(color: AppTheme.textPrimary, fontSize: 16, fontWeight: FontWeight.w600, height: 1.4),
                  ),
                  const SizedBox(height: 8),
                  Text('Confidence: ${(insight['confidence'] * 100).toInt()}% • Modeled on individual baseline history',
                      style: const TextStyle(color: AppTheme.cyanMuted, fontSize: 12)),
                ],
              ),
            ),
            const SizedBox(height: 20),
            _buildSectionCard(
              title: 'Grounded Observations',
              icon: Icons.checklist_rtl,
              iconColor: AppTheme.cyan,
              items: observations,
            ),
            const SizedBox(height: 16),
            _buildSectionCard(
              title: 'Physiological Hypotheses (Non-Diagnostic)',
              icon: Icons.psychology,
              iconColor: AppTheme.indigo,
              items: hypotheses,
            ),
            const SizedBox(height: 16),
            _buildSectionCard(
              title: 'Actionable Guidance',
              icon: Icons.directions_run,
              iconColor: AppTheme.success,
              items: actions,
            ),
            const SizedBox(height: 16),
            _buildEvidenceSection(insight['evidence'] as List<dynamic>? ?? []),
            const SizedBox(height: 16),
            HealthcareCard(
              onTap: () => Navigator.of(context).push(MaterialPageRoute(builder: (_) => const EvidenceScreen())),
              borderColor: AppTheme.cyan.withValues(alpha: 0.3),
              child: const Row(
                children: [
                  Icon(Icons.search, color: AppTheme.cyan, size: 24),
                  SizedBox(width: 14),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text('Explore Biomedical Literature (Tavily)', style: TextStyle(color: AppTheme.textPrimary, fontSize: 15, fontWeight: FontWeight.w700)),
                        SizedBox(height: 2),
                        Text('Perform live bounded queries across PubMed, NIH & Nature Medicine.', style: TextStyle(color: AppTheme.textSecondary, fontSize: 12)),
                      ],
                    ),
                  ),
                  Icon(Icons.chevron_right, color: AppTheme.textMuted),
                ],
              ),
            ),
            const SizedBox(height: 24),
            Container(
              padding: const EdgeInsets.all(14),
              decoration: BoxDecoration(
                color: AppTheme.surfaceLight,
                borderRadius: BorderRadius.circular(12),
                border: Border.all(color: AppTheme.cardBorder),
              ),
              child: Text(
                insight['disclaimer'],
                style: const TextStyle(color: AppTheme.textMuted, fontSize: 11, height: 1.4),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildSectionCard({
    required String title,
    required IconData icon,
    required Color iconColor,
    required List<dynamic> items,
  }) {
    return HealthcareCard(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Icon(icon, color: iconColor, size: 18),
              const SizedBox(width: 8),
              Text(title, style: const TextStyle(color: AppTheme.textPrimary, fontSize: 15, fontWeight: FontWeight.w700)),
            ],
          ),
          const SizedBox(height: 12),
          ...items.map((it) => Padding(
                padding: const EdgeInsets.only(bottom: 8),
                child: Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Text('• ', style: TextStyle(color: AppTheme.cyan, fontSize: 14, fontWeight: FontWeight.bold)),
                    Expanded(child: Text(it.toString(), style: const TextStyle(color: AppTheme.textSecondary, fontSize: 13, height: 1.35))),
                  ],
                ),
              )),
        ],
      ),
    );
  }

  Widget _buildEvidenceSection(List<dynamic> evidenceItems) {
    if (evidenceItems.isEmpty) return const SizedBox.shrink();

    return HealthcareCard(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Row(
            children: [
              Icon(Icons.menu_book, color: AppTheme.cyan, size: 18),
              SizedBox(width: 8),
              Text(
                'Biomedical Literature & Evidence (Tavily Grounded)',
                style: TextStyle(color: AppTheme.textPrimary, fontSize: 15, fontWeight: FontWeight.w700),
              ),
            ],
          ),
          const SizedBox(height: 4),
          const Text(
            'Sources automatically retrieved, domain-filtered, and ranked to ground AI interpretations without hallucination.',
            style: TextStyle(color: AppTheme.textMuted, fontSize: 12),
          ),
          const SizedBox(height: 14),
          ...evidenceItems.map((item) {
            final e = item as Map<String, dynamic>;
            final title = e['title'] ?? 'Scientific Study';
            final domain = e['domain'] ?? 'ncbi.nlm.nih.gov';
            final excerpt = e['relevantExcerpt'] ?? e['relevance'] ?? '';
            final score = e['relevanceScore'] != null
                ? '${((e['relevanceScore'] as num) * 100).toInt()}% match'
                : 'Verified Reference';

            return Container(
              margin: const EdgeInsets.only(bottom: 12),
              padding: const EdgeInsets.all(12),
              decoration: BoxDecoration(
                color: AppTheme.surfaceLight,
                borderRadius: BorderRadius.circular(10),
                border: Border.all(color: AppTheme.cardBorder),
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Expanded(
                        child: Text(
                          title,
                          style: const TextStyle(
                            color: AppTheme.textPrimary,
                            fontSize: 13,
                            fontWeight: FontWeight.w700,
                            height: 1.3,
                          ),
                        ),
                      ),
                      const SizedBox(width: 8),
                      HealthcareBadge(label: score, color: AppTheme.cyan),
                    ],
                  ),
                  const SizedBox(height: 6),
                  Row(
                    children: [
                      const Icon(Icons.public, color: AppTheme.textSecondary, size: 12),
                      const SizedBox(width: 4),
                      Text(
                        domain,
                        style: const TextStyle(color: AppTheme.textSecondary, fontSize: 11, fontWeight: FontWeight.w600),
                      ),
                    ],
                  ),
                  if (excerpt.isNotEmpty) ...[
                    const SizedBox(height: 8),
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 8),
                      decoration: BoxDecoration(
                        color: AppTheme.background.withValues(alpha: 0.6),
                        borderRadius: BorderRadius.circular(6),
                        border: Border(left: BorderSide(color: AppTheme.cyan.withValues(alpha: 0.8), width: 3)),
                      ),
                      child: Text(
                        '"$excerpt"',
                        style: const TextStyle(
                          color: AppTheme.textSecondary,
                          fontSize: 12,
                          fontStyle: FontStyle.italic,
                          height: 1.35,
                        ),
                      ),
                    ),
                  ],
                ],
              ),
            );
          }),
        ],
      ),
    );
  }
}
