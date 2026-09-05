package com.ruoyi.cost.boq.match.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;
import com.ruoyi.cost.boq.domain.CostBoqItem;
import com.ruoyi.cost.boq.match.domain.CostBoqCompare;
import com.ruoyi.cost.boq.match.support.BoqCompareCalculator;
import com.ruoyi.cost.boq.match.support.BoqMatchType;
import com.ruoyi.cost.boq.match.support.BoqTextNormalizer;

/** 非AI的一对一清单匹配引擎。 */
@Component
public class BoqMatchEngine
{
    private static final double LOW_THRESHOLD = 0.70D;
    private static final double HIGH_THRESHOLD = 0.88D;
    private static final int SMALL_SET_SCAN_LIMIT = 2000;
    private static final int MAX_FUZZY_CANDIDATES = 500;
    private final BoqTextNormalizer normalizer;
    private final BoqCompareCalculator calculator;

    public BoqMatchEngine(BoqTextNormalizer normalizer, BoqCompareCalculator calculator)
    {
        this.normalizer = normalizer;
        this.calculator = calculator;
    }

    public List<CostBoqCompare> match(Long projectId, Long leftBatchId, Long rightBatchId,
            List<CostBoqItem> leftItems, List<CostBoqItem> rightItems,
            Set<Long> reservedLeftIds, Set<Long> reservedRightIds, String operator)
    {
        List<CandidateItem> left = wrap(leftItems, reservedLeftIds);
        List<CandidateItem> right = wrap(rightItems, reservedRightIds);
        Set<Long> usedLeft = new HashSet<>();
        Set<Long> usedRight = new HashSet<>();
        List<CostBoqCompare> results = new ArrayList<>();

        matchExactCode(projectId, leftBatchId, rightBatchId, left, right,
                usedLeft, usedRight, results, operator);
        matchExactNameAndUnit(projectId, leftBatchId, rightBatchId, left, right,
                usedLeft, usedRight, results, operator);
        matchFuzzy(projectId, leftBatchId, rightBatchId, left, right,
                usedLeft, usedRight, results, operator);

        for (CandidateItem item : left)
        {
            if (!usedLeft.contains(item.item.getId()))
            {
                results.add(calculator.create(projectId, leftBatchId, rightBatchId,
                        item.item, null, BoqMatchType.ONLY_LEFT, 0D, operator));
            }
        }
        for (CandidateItem item : right)
        {
            if (!usedRight.contains(item.item.getId()))
            {
                results.add(calculator.create(projectId, leftBatchId, rightBatchId,
                        null, item.item, BoqMatchType.ONLY_RIGHT, 0D, operator));
            }
        }
        return results;
    }

    private void matchExactCode(Long projectId, Long leftBatchId, Long rightBatchId,
            List<CandidateItem> left, List<CandidateItem> right, Set<Long> usedLeft,
            Set<Long> usedRight, List<CostBoqCompare> results, String operator)
    {
        Map<String, List<CandidateItem>> rightByCode = new HashMap<>();
        for (CandidateItem item : right)
        {
            if (!item.code.isEmpty()) rightByCode.computeIfAbsent(item.code, key -> new ArrayList<>()).add(item);
        }
        for (CandidateItem leftItem : left)
        {
            if (leftItem.code.isEmpty()) continue;
            CandidateItem best = rightByCode.getOrDefault(leftItem.code, List.of()).stream()
                    .filter(item -> !usedRight.contains(item.item.getId()))
                    .max(Comparator.comparingDouble(item -> secondaryScore(leftItem, item))).orElse(null);
            if (best != null)
            {
                addMatch(projectId, leftBatchId, rightBatchId, leftItem, best,
                        BoqMatchType.EXACT, 1D, usedLeft, usedRight, results, operator);
            }
        }
    }

    private void matchExactNameAndUnit(Long projectId, Long leftBatchId, Long rightBatchId,
            List<CandidateItem> left, List<CandidateItem> right, Set<Long> usedLeft,
            Set<Long> usedRight, List<CostBoqCompare> results, String operator)
    {
        Map<String, List<CandidateItem>> rightByNameUnit = new HashMap<>();
        for (CandidateItem item : right)
        {
            if (!item.name.isEmpty() && !usedRight.contains(item.item.getId()))
            {
                rightByNameUnit.computeIfAbsent(item.name + '\u0000' + item.unit,
                        key -> new ArrayList<>()).add(item);
            }
        }
        for (CandidateItem leftItem : left)
        {
            if (leftItem.name.isEmpty() || usedLeft.contains(leftItem.item.getId())) continue;
            CandidateItem best = rightByNameUnit.getOrDefault(leftItem.name + '\u0000' + leftItem.unit,
                    List.of()).stream().filter(item -> !usedRight.contains(item.item.getId()))
                    .max(Comparator.comparingDouble(item -> featureScore(leftItem, item))).orElse(null);
            if (best != null)
            {
                addMatch(projectId, leftBatchId, rightBatchId, leftItem, best,
                        BoqMatchType.EXACT, 1D, usedLeft, usedRight, results, operator);
            }
        }
    }

    private void matchFuzzy(Long projectId, Long leftBatchId, Long rightBatchId,
            List<CandidateItem> left, List<CandidateItem> right, Set<Long> usedLeft,
            Set<Long> usedRight, List<CostBoqCompare> results, String operator)
    {
        List<CandidateItem> remainingRight = right.stream()
                .filter(item -> !usedRight.contains(item.item.getId())).toList();
        Map<String, List<CandidateItem>> gramIndex = buildGramIndex(remainingRight);
        List<Edge> edges = new ArrayList<>();
        for (CandidateItem leftItem : left)
        {
            if (usedLeft.contains(leftItem.item.getId()) || leftItem.name.isEmpty()) continue;
            Collection<CandidateItem> candidates = remainingRight.size() <= SMALL_SET_SCAN_LIMIT
                    ? remainingRight : candidates(leftItem, gramIndex);
            for (CandidateItem rightItem : candidates)
            {
                if (!unitCompatible(leftItem.unit, rightItem.unit)) continue;
                double nameScore = normalizer.similarity(leftItem.name, rightItem.name);
                if (nameScore < 0.55D) continue;
                double score = 0.75D * nameScore + 0.15D * featureScore(leftItem, rightItem)
                        + 0.10D * unitScore(leftItem.unit, rightItem.unit);
                if (score >= LOW_THRESHOLD) edges.add(new Edge(leftItem, rightItem, score));
            }
        }
        edges.sort(Comparator.comparingDouble(Edge::score).reversed()
                .thenComparing(edge -> sourceOrder(edge.left.item))
                .thenComparing(edge -> sourceOrder(edge.right.item)));
        for (Edge edge : edges)
        {
            if (usedLeft.contains(edge.left.item.getId()) || usedRight.contains(edge.right.item.getId())) continue;
            BoqMatchType type = edge.score >= HIGH_THRESHOLD
                    ? BoqMatchType.HIGH_SIMILARITY : BoqMatchType.LOW_SIMILARITY;
            addMatch(projectId, leftBatchId, rightBatchId, edge.left, edge.right,
                    type, edge.score, usedLeft, usedRight, results, operator);
        }
    }

    private Collection<CandidateItem> candidates(CandidateItem left,
            Map<String, List<CandidateItem>> gramIndex)
    {
        List<List<CandidateItem>> postings = grams(left.name).stream()
                .map(gramIndex::get).filter(java.util.Objects::nonNull)
                .sorted(Comparator.comparingInt(List::size)).limit(4).toList();
        LinkedHashSet<CandidateItem> result = new LinkedHashSet<>();
        for (List<CandidateItem> posting : postings)
        {
            for (CandidateItem item : posting)
            {
                result.add(item);
                if (result.size() >= MAX_FUZZY_CANDIDATES) return result;
            }
        }
        return result;
    }

    private Map<String, List<CandidateItem>> buildGramIndex(List<CandidateItem> items)
    {
        Map<String, List<CandidateItem>> index = new HashMap<>();
        for (CandidateItem item : items)
        {
            for (String gram : grams(item.name)) index.computeIfAbsent(gram, key -> new ArrayList<>()).add(item);
        }
        return index;
    }

    private Set<String> grams(String value)
    {
        Set<String> grams = new LinkedHashSet<>();
        if (value.length() < 2)
        {
            if (!value.isEmpty()) grams.add(value);
            return grams;
        }
        for (int i = 0; i < value.length() - 1; i++) grams.add(value.substring(i, i + 2));
        return grams;
    }

    private void addMatch(Long projectId, Long leftBatchId, Long rightBatchId,
            CandidateItem left, CandidateItem right, BoqMatchType type, double score,
            Set<Long> usedLeft, Set<Long> usedRight, List<CostBoqCompare> results, String operator)
    {
        usedLeft.add(left.item.getId());
        usedRight.add(right.item.getId());
        results.add(calculator.create(projectId, leftBatchId, rightBatchId,
                left.item, right.item, type, score, operator));
    }

    private double secondaryScore(CandidateItem left, CandidateItem right)
    {
        return 0.65D * normalizer.similarity(left.name, right.name)
                + 0.25D * featureScore(left, right) + 0.10D * unitScore(left.unit, right.unit);
    }

    private double featureScore(CandidateItem left, CandidateItem right)
    {
        if (left.feature.isEmpty() && right.feature.isEmpty()) return 1D;
        if (left.feature.isEmpty() || right.feature.isEmpty()) return 0.5D;
        return normalizer.similarity(left.feature, right.feature);
    }

    private boolean unitCompatible(String left, String right)
    {
        return left.isEmpty() || right.isEmpty() || left.equals(right);
    }

    private double unitScore(String left, String right)
    {
        if (left.isEmpty() || right.isEmpty()) return 0.5D;
        return left.equals(right) ? 1D : 0D;
    }

    private List<CandidateItem> wrap(List<CostBoqItem> items, Set<Long> reservedIds)
    {
        return items.stream().filter(item -> !reservedIds.contains(item.getId()))
                .sorted(Comparator.comparingInt(this::sourceOrder))
                .map(item -> new CandidateItem(item, normalizer.normalizeCode(item.getItemCode()),
                        normalizer.normalizeText(item.getItemName()),
                        normalizer.normalizeText(item.getItemFeature()),
                        normalizer.normalizeUnit(item.getUnit()))).toList();
    }

    private int sourceOrder(CostBoqItem item)
    {
        return item.getSourceRow() == null ? Integer.MAX_VALUE : item.getSourceRow();
    }

    private record CandidateItem(CostBoqItem item, String code, String name, String feature, String unit) {}
    private record Edge(CandidateItem left, CandidateItem right, double score) {}
}
