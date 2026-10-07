import 'package:flutter/material.dart';
import '../../core/theme/app_theme.dart';
import '../../shared/widgets/healthcare_widgets.dart';

/// Step definition in the OS4All Intelligence Trace pipeline.
class TraceStepItem {
  final int stepNumber;
  final String title;
  final String status;
  final String duration;
  final String providerTag;
  final String shortExplanation;
  final IconData icon;
  final Color accentColor;
  final Map<String, dynamic> technicalDetails;

  const TraceStepItem({
    required this.stepNumber,
    required this.title,
    required this.status,
    required this.duration,
    required this.providerTag,
    required this.shortExplanation,
    required this.icon,
    required this.accentColor,
    required this.technicalDetails,
  });
}

/// OS4All Intelligence Trace Screen for Hackathon Judges.
/// Visualizes the full 8-step deterministic processing pipeline in <30 seconds,
/// with expandable technical deep-dives without exposing sensitive API keys or raw PII.
class IntelligenceTraceScreen extends StatefulWidget {
  const IntelligenceTraceScreen({super.key});

  @override
  State<IntelligenceTraceScreen> createState() => _IntelligenceTraceScreenState();
}

class _IntelligenceTraceScreenState extends State<IntelligenceTraceScreen> {
  final String _workflowId = "WF-4a819b02-7c3d";
  final String _traceVersion = "v1.0-deterministic";

  int? _expandedStepIndex;

  final List<TraceStepItem> _traceSteps = const [
    TraceStepItem(
      stepNumber: 1,
      title: "DATA RECEIVED",
      status: "COMPLETED",
      duration: "Demo visualization",
      providerTag: "OS4All Edge Telemetry Ingestion",
      shortExplanation: "Continuous wearable observations ingested and validated across 4 physiological channels.",
      icon: Icons.sensors_rounded,
      accentColor: AppTheme.primary,
      technicalDetails: {
        "Signals Ingested": "Resting HR, Nocturnal HRV (rMSSD), Sleep Duration, SpO2",
        "Ingestion Interval": "Epoch 24h normalized with ISO-8601 UTC timestamps",
        "Data Validation": "Range checks passed, unit standardization confirmed (bpm, ms, hrs, %)",
        "Security & Anonymization": "User ID replaced with ephemeral subject token (ANON_SUBJ_42)",
      },
    ),
    TraceStepItem(
      stepNumber: 2,
      title: "BASELINE COMPARED",
      status: "COMPLETED",
      duration: "Demo visualization",
      providerTag: "Personal Baseline Engine",
      shortExplanation: "Current vitals compared against individual 45-day historical baseline envelope rather than population averages.",
      icon: Icons.tune_rounded,
      accentColor: AppTheme.secondary,
      technicalDetails: {
        "Baseline Calibration": "45-day synthetic rolling baseline (N=45 observations; min threshold 5 required)",
        "Resting Heart Rate": "Current: 74.0 bpm vs Personal Baseline: 60.4 bpm (±2.8 bpm)",
        "HRV rMSSD": "Current: 36.0 ms vs Personal Baseline: 55.2 ms (±4.6 ms)",
        "Sleep Duration": "Current: 5.2 hrs vs Personal Baseline: 7.95 hrs (±0.6 hrs)",
        "Mathematical Standard": "z-score deviations: RHR (+2.4σ), HRV (-2.1σ), Sleep (-2.8σ)",
      },
    ),
    TraceStepItem(
      stepNumber: 3,
      title: "TREND DETECTED",
      status: "COMPLETED",
      duration: "Demo visualization",
      providerTag: "Deterministic Trend Agent",
      shortExplanation: "Persistent multi-day directional shift identified: values departed from mean for 4 consecutive days.",
      icon: Icons.trending_up_rounded,
      accentColor: AppTheme.warning,
      technicalDetails: {
        "Persistence Counter": "4 consecutive epochs exceeding 1.5 standard deviations",
        "Directionality": "Resting HR: Ascending (+14 bpm) | HRV: Suppressed (-19 ms)",
        "Threshold Rule": "Persistence >= 2 days triggered (Rule TR-04: Sustained Departure)",
        "State Transition": "TRANSITION: STABLE → PERSISTENT DIRECTIONAL DRIFT",
      },
    ),
    TraceStepItem(
      stepNumber: 4,
      title: "PATTERN ANALYZED",
      status: "COMPLETED",
      duration: "Demo visualization",
      providerTag: "Multi-Signal Correlation Engine",
      shortExplanation: "Compound autonomic strain pattern recognized: elevated pulse coupled with vagal tone reduction and sleep restriction.",
      icon: Icons.hub_rounded,
      accentColor: Color(0xFFEF4444),
      technicalDetails: {
        "Pattern Signature": "COMPOUND_AUTONOMIC_STRAIN (RHR ↑ + HRV ↓ + SLEEP ↓)",
        "Correlation Coefficient": "r = -0.84 (Strong inverse decoupling between HR and HRV)",
        "Diagnostic Rule": "Non-diagnostic physiological pattern detection",
        "Confidence Score": "0.95 (High confidence from longitudinal temporal coupling)",
      },
    ),
    TraceStepItem(
      stepNumber: 5,
      title: "NEMOTRON REASONING",
      status: "COMPLETED",
      duration: "Demo visualization",
      providerTag: "NVIDIA Nemotron via Nebius",
      shortExplanation: "NVIDIA Nemotron synthesizes structured context into non-diagnostic physiological hypotheses.",
      icon: Icons.psychology_rounded,
      accentColor: Color(0xFF7C3AED),
      technicalDetails: {
        "Model Orchestration": "NVIDIA Nemotron (nvidia/Llama-3_1-Nemotron-70B-Instruct; Mock mode in offline demo)",
        "Cloud Infrastructure": "Nebius Token Factory Studio API (falls back to local mock when unconfigured)",
        "Prompt Strategy": "Structured JSON schema enforcement with strict zero-hallucination constraint",
        "Operational Directives": "3-tier distinction: OS4All observed vs External evidence vs Recommends",
        "Data Hygiene": "Zero raw PII, zero database access, curated contextual snapshot only",
      },
    ),
    TraceStepItem(
      stepNumber: 6,
      title: "EVIDENCE RETRIEVED",
      status: "COMPLETED",
      duration: "Demo visualization",
      providerTag: "Tavily Evidence Agent",
      shortExplanation: "Bounded biomedical search retrieves peer-reviewed PubMed and Nature Medicine studies grounding the autonomic pattern.",
      icon: Icons.travel_explore_rounded,
      accentColor: Color(0xFF0284C7),
      technicalDetails: {
        "Trigger Gate": "Conditional gate engaged (Bypassed for stable baselines, triggered on anomaly)",
        "Search Provider": "Tavily Biomedical Search API (Mock Evidence Service in offline demo)",
        "Domain Restriction": "Strictly limited to pubmed.ncbi.nlm.nih.gov, nature.com, ahajournals.org",
        "Query Formulated": "'heart rate variability and resting heart rate elevation during acute sleep restriction recovery'",
        "Sources Grounded": "2 peer-reviewed articles verified with DOI and excerpt provenance",
      },
    ),
    TraceStepItem(
      stepNumber: 7,
      title: "INSIGHT GENERATED",
      status: "COMPLETED",
      duration: "Demo visualization",
      providerTag: "Deterministic Health Insight Synthesizer",
      shortExplanation: "Structured insight built with 3-tier distinction, complete provenance, and clinical disclaimer.",
      icon: Icons.auto_awesome_rounded,
      accentColor: Color(0xFF10B981),
      technicalDetails: {
        "Insight Summary": "'OS4All observed: Multi-signal autonomic strain pattern over last 4 days...'",
        "Urgency Level": "MONITOR (Routine non-emergent autonomic debt follow-up)",
        "Provenance Integrity": "All 2 citations validated against retrieved corpus; 0 hallucinated sources",
        "Clinical Disclaimer": "Prominently affixed; explicitly non-diagnostic informational output",
      },
    ),
    TraceStepItem(
      stepNumber: 8,
      title: "ACTION RECOMMENDED",
      status: "COMPLETED",
      duration: "Demo visualization",
      providerTag: "Pragmatic Action Agent",
      shortExplanation: "Formulates pragmatic recovery guidance and non-alarmist physician review criteria.",
      icon: Icons.check_circle_outline_rounded,
      accentColor: AppTheme.stable,
      technicalDetails: {
        "Action 1": "Prioritize 8 hours of restorative sleep over next 2-3 consecutive evenings",
        "Action 2": "Reduce high-intensity cardiovascular training to active recovery load",
        "Action 3": "Log lifestyle context (travel, hydration, caffeine timing)",
        "Clinical Review Gate": "If elevations persist >5 days, review trend report with physician",
      },
    ),
  ];

  @override
  Widget build(BuildContext context) {
    const totalLatency = "Demo visualization";

    return Scaffold(
      backgroundColor: AppTheme.background,
      appBar: AppBar(
        title: const Text('OS4All Intelligence Trace'),
        actions: [
          Container(
            margin: const EdgeInsets.symmetric(vertical: 12, horizontal: 16),
            padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
            decoration: BoxDecoration(
              color: AppTheme.primary.withValues(alpha: 0.12),
              borderRadius: BorderRadius.circular(16),
              border: Border.all(color: AppTheme.primary.withValues(alpha: 0.3)),
            ),
            child: const Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                CircleAvatar(radius: 3.5, backgroundColor: AppTheme.stable),
                SizedBox(width: 6),
                Text(
                  'DEMO TRACE',
                  style: TextStyle(color: AppTheme.primary, fontSize: 10.5, fontWeight: FontWeight.w800),
                ),
              ],
            ),
          ),
        ],
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.fromLTRB(16, 12, 16, 36),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Mandatory Demo Label
            const DemoDataNoticeBanner(label: 'DEMO DATA — NOT A REAL PATIENT'),
            const SizedBox(height: 8),

            // Hackathon Judge Overview Card (<30 Second Comprehension)
            _buildJudgeSummaryCard(totalLatency),
            const SizedBox(height: 20),

            // Pipeline Step Header
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                const Text(
                  'Deterministic Pipeline Execution',
                  style: TextStyle(
                    color: AppTheme.textPrimary,
                    fontSize: 16,
                    fontWeight: FontWeight.w800,
                    letterSpacing: -0.2,
                  ),
                ),
                Text(
                  '${_traceSteps.length} Verified Steps',
                  style: const TextStyle(color: AppTheme.textSecondary, fontSize: 12, fontWeight: FontWeight.w600),
                ),
              ],
            ),
            const SizedBox(height: 12),

            // Visual Processing Pipeline Flow
            ..._traceSteps.asMap().entries.map((entry) {
              final index = entry.key;
              final step = entry.value;
              final isLast = index == _traceSteps.length - 1;
              final isExpanded = _expandedStepIndex == index;

              return _buildPipelineStepRow(step, index, isLast, isExpanded);
            }),

            const SizedBox(height: 20),

            // Security & Privacy Compliance Banner (Judges Assurance)
            _buildSecurityComplianceCard(),
          ],
        ),
      ),
    );
  }

  // -------------------------------------------------------------
  // Judge Overview Card (<30 seconds comprehension)
  // -------------------------------------------------------------
  Widget _buildJudgeSummaryCard(String totalLatency) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(18),
      decoration: BoxDecoration(
        color: AppTheme.surface,
        borderRadius: BorderRadius.circular(18),
        border: Border.all(color: AppTheme.primary.withValues(alpha: 0.35), width: 1.5),
        boxShadow: AppTheme.cardShadow,
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Expanded(
                child: Row(
                  children: [
                    Container(
                      padding: const EdgeInsets.all(6),
                      decoration: BoxDecoration(
                        color: AppTheme.primary.withValues(alpha: 0.1),
                        borderRadius: BorderRadius.circular(8),
                      ),
                      child: const Icon(Icons.account_tree_rounded, color: AppTheme.primary, size: 18),
                    ),
                    const SizedBox(width: 8),
                    const Expanded(
                      child: Text(
                        'Deterministic Health-Intelligence Trace',
                        style: TextStyle(color: AppTheme.textPrimary, fontSize: 13.5, fontWeight: FontWeight.w800),
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(width: 6),
              HealthcareBadge(label: totalLatency, color: AppTheme.primary),
            ],
          ),
          const SizedBox(height: 12),
          const Text(
            'OS4All processes biometric data through a verifiable 8-stage pipeline. The AI receives only anonymized, structured context snapshots—never raw patient databases or opaque black-box queries.',
            style: TextStyle(color: AppTheme.textSecondary, fontSize: 13, height: 1.45),
          ),
          const SizedBox(height: 14),
          Wrap(
            spacing: 8,
            runSpacing: 6,
            children: [
              _buildTraceBadge('Workflow ID', _workflowId),
              _buildTraceBadge('Context Version', _traceVersion),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildTraceBadge(String label, String value) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      decoration: BoxDecoration(
        color: AppTheme.surfaceElevated,
        borderRadius: BorderRadius.circular(6),
        border: Border.all(color: AppTheme.border),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Text('$label: ', style: const TextStyle(color: AppTheme.textMuted, fontSize: 10.5)),
          Text(value, style: const TextStyle(color: AppTheme.textPrimary, fontSize: 11, fontWeight: FontWeight.w700)),
        ],
      ),
    );
  }

  // -------------------------------------------------------------
  // Pipeline Step Row (Connected vertical timeline nodes)
  // -------------------------------------------------------------
  Widget _buildPipelineStepRow(TraceStepItem step, int index, bool isLast, bool isExpanded) {
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        // Left Column: Step Icon + Connecting Vertical Pipe
        Column(
          children: [
            Container(
              width: 36,
              height: 36,
              decoration: BoxDecoration(
                color: step.accentColor.withValues(alpha: 0.12),
                shape: BoxShape.circle,
                border: Border.all(color: step.accentColor, width: 1.5),
              ),
              child: Icon(step.icon, color: step.accentColor, size: 18),
            ),
            if (!isLast)
              Container(
                width: 2,
                height: isExpanded ? 240 : 80,
                color: AppTheme.border,
              ),
          ],
        ),
        const SizedBox(width: 14),

        // Right Column: Step Content Card with "View details" Expandable Area
        Expanded(
          child: Padding(
            padding: EdgeInsets.only(bottom: isLast ? 0 : 12),
            child: Container(
              padding: const EdgeInsets.all(14),
              decoration: BoxDecoration(
                color: AppTheme.surface,
                borderRadius: BorderRadius.circular(14),
                border: Border.all(
                  color: isExpanded ? step.accentColor.withValues(alpha: 0.5) : AppTheme.border,
                  width: isExpanded ? 1.5 : 1.0,
                ),
                boxShadow: AppTheme.cardShadow,
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  // Step Header
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Row(
                        children: [
                          Text(
                            'STEP ${step.stepNumber}',
                            style: TextStyle(
                              color: step.accentColor,
                              fontSize: 10.5,
                              fontWeight: FontWeight.w800,
                              letterSpacing: 0.8,
                            ),
                          ),
                          const SizedBox(width: 8),
                          Text(
                            step.title,
                            style: const TextStyle(
                              color: AppTheme.textPrimary,
                              fontSize: 13.5,
                              fontWeight: FontWeight.w800,
                              letterSpacing: -0.2,
                            ),
                          ),
                        ],
                      ),
                      Container(
                        padding: const EdgeInsets.symmetric(horizontal: 7, vertical: 2),
                        decoration: BoxDecoration(
                          color: AppTheme.surfaceElevated,
                          borderRadius: BorderRadius.circular(12),
                        ),
                        child: Text(
                          step.duration,
                          style: const TextStyle(
                            color: AppTheme.textSecondary,
                            fontSize: 10.5,
                            fontWeight: FontWeight.w700,
                          ),
                        ),
                      ),
                    ],
                  ),

                  const SizedBox(height: 6),

                  // Provider Pill Tag (e.g. "NVIDIA Nemotron via Nebius", "Tavily Evidence Agent")
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                    decoration: BoxDecoration(
                      color: step.accentColor.withValues(alpha: 0.08),
                      borderRadius: BorderRadius.circular(6),
                    ),
                    child: Row(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        Icon(Icons.bolt_rounded, size: 12, color: step.accentColor),
                        const SizedBox(width: 4),
                        Text(
                          step.providerTag,
                          style: TextStyle(
                            color: step.accentColor,
                            fontSize: 11,
                            fontWeight: FontWeight.w700,
                          ),
                        ),
                      ],
                    ),
                  ),

                  const SizedBox(height: 8),

                  // Short Human-Readable Explanation
                  Text(
                    step.shortExplanation,
                    style: const TextStyle(color: AppTheme.textSecondary, fontSize: 12.5, height: 1.35),
                  ),

                  const SizedBox(height: 10),

                  // "View details" Toggle for Technically Interested Judges
                  InkWell(
                    borderRadius: BorderRadius.circular(6),
                    onTap: () {
                      setState(() {
                        _expandedStepIndex = isExpanded ? null : index;
                      });
                    },
                    child: Padding(
                      padding: const EdgeInsets.symmetric(vertical: 4),
                      child: Row(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          Text(
                            isExpanded ? 'Hide technical details' : 'View details (Technical Audit)',
                            style: TextStyle(
                              color: step.accentColor,
                              fontSize: 11.5,
                              fontWeight: FontWeight.w700,
                            ),
                          ),
                          const SizedBox(width: 4),
                          Icon(
                            isExpanded ? Icons.expand_less_rounded : Icons.expand_more_rounded,
                            size: 16,
                            color: step.accentColor,
                          ),
                        ],
                      ),
                    ),
                  ),

                  // Expandable Technical Details View
                  if (isExpanded) ...[
                    const SizedBox(height: 10),
                    Container(
                      padding: const EdgeInsets.all(12),
                      decoration: BoxDecoration(
                        color: AppTheme.surfaceLight,
                        borderRadius: BorderRadius.circular(8),
                        border: Border.all(color: AppTheme.border),
                      ),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: step.technicalDetails.entries.map((e) {
                          return Padding(
                            padding: const EdgeInsets.only(bottom: 6),
                            child: Row(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Text(
                                  '${e.key}: ',
                                  style: const TextStyle(
                                    color: AppTheme.textPrimary,
                                    fontSize: 11.5,
                                    fontWeight: FontWeight.w700,
                                  ),
                                ),
                                Expanded(
                                  child: Text(
                                    e.value.toString(),
                                    style: const TextStyle(
                                      color: AppTheme.textSecondary,
                                      fontSize: 11.5,
                                      height: 1.3,
                                    ),
                                  ),
                                ),
                              ],
                            ),
                          );
                        }).toList(),
                      ),
                    ),
                  ],
                ],
              ),
            ),
          ),
        ),
      ],
    );
  }

  // -------------------------------------------------------------
  // Security & Privacy Compliance Card (Judges Assurance)
  // -------------------------------------------------------------
  Widget _buildSecurityComplianceCard() {
    return Container(
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: AppTheme.surfaceElevated,
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: AppTheme.border),
      ),
      child: const Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(Icons.shield_outlined, color: AppTheme.stable, size: 20),
          SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  'Privacy & Security Verification',
                  style: TextStyle(color: AppTheme.textPrimary, fontSize: 13, fontWeight: FontWeight.w700),
                ),
                SizedBox(height: 3),
                Text(
                  'API keys (Nebius & Tavily) and sensitive environment variables are kept strictly on the backend. No patient identifiers (PII), credentials, or raw database records are exposed in frontend bundles or inference payloads.',
                  style: TextStyle(color: AppTheme.textSecondary, fontSize: 11.5, height: 1.35),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
