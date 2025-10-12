package com.example.recommendation.service.lsh;

import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class CandidateGenerator {
    public Set<String> generateCandidates(Map<String, List<Integer>> buckets) {
        Set<String> candidates = new HashSet<>();

        for (List<Integer> users : buckets.values()) {
            if (users.size() > 1) {
                for (int i = 0; i < users.size(); i++) {
                    for (int j = i + 1; j < users.size(); j++) {
                        int u1 = users.get(i);
                        int u2 = users.get(j);
                        String pair = (u1 < u2) ? u1 + "-" + u2 : u2 + "-" + u1;
                        candidates.add(pair);
                    }
                }
            }
        }
        return candidates;
    }
}
