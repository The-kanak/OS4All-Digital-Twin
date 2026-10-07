import 'package:flutter/material.dart';
import 'clinical_timeline_screen.dart';

/// Legacy TimelineScreen mapped to modern ClinicalTimelineScreen
class TimelineScreen extends StatelessWidget {
  const TimelineScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return const ClinicalTimelineScreen();
  }
}
