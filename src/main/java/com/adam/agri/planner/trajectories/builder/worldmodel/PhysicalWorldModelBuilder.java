package com.adam.agri.planner.trajectories.builder.worldmodel;

import com.adam.agri.planner.symbolic.ontology.upper.*;

import java.util.*;

/**
 * Builder for physical world models.
 */
public class PhysicalWorldModelBuilder {

    private final List<Physical> physicalEntities;
    private final List<SpatialRelation> spatialRelations;
    private final Map<String, Object> globalProperties;

    public PhysicalWorldModelBuilder() {
        this.physicalEntities = new ArrayList<>();
        this.spatialRelations = new ArrayList<>();
        this.globalProperties = new HashMap<>();
    }

    public PhysicalWorldModelBuilder addPhysicalEntity(Physical entity) {
        physicalEntities.add(entity);
        return this;
    }

    public PhysicalWorldModelBuilder fromObservationEntities(Collection<Entity> entities) {
        for (Entity entity : entities) {
            if (entity instanceof Physical) {
                physicalEntities.add((Physical) entity);
            }
        }
        return this;
    }

    public PhysicalWorldModelBuilder parseNaturalLanguage(String description) {
        String lower = description.toLowerCase();

        if (lower.contains("at") && (lower.contains("(") || lower.contains("position"))) {
            extractObjectLocations(description);
        }

        if (lower.contains("room") || lower.contains("area") || lower.contains("space")) {
            extractRoomDescription(description);
        }

        return this;
    }

    public PhysicalWorldModel build() {
        return new PhysicalWorldModel(physicalEntities, spatialRelations, new HashMap<>(globalProperties));
    }

    private void extractObjectLocations(String description) {
        String pattern = "(\\w+)\\s+(?:is\\s+)?(?:at|located at|in)\\s*\\(?\\s*(-?\\d+(?:\\.\\d+)?)\\s*,?\\s*(-?\\d+(?:\\.\\d+)?)\\s*(?:,\\s*(-?\\d+(?:\\.\\d+)?))?\\s*\\)?";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern, java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher m = p.matcher(description);

        while (m.find()) {
            String objectName = m.group(1);
            double x = Double.parseDouble(m.group(2));
            double y = Double.parseDouble(m.group(3));
            double z = m.group(4) != null ? Double.parseDouble(m.group(4)) : 0.0;

            Location loc = new SimplePoint(x, y, z);
            PhysicalObject obj = new PhysicalObject(
                EntityId.of(objectName.toLowerCase()),
                Collections.emptySet(),
                loc,
                new SimpleInterval(0, Double.MAX_VALUE),
                objectName
            );
            physicalEntities.add(obj);
        }
    }

    private void extractRoomDescription(String description) {
        String lower = description.toLowerCase();
        if (lower.contains("room")) {
            globalProperties.put("has_room", true);
        }
    }

    private static class SimplePoint implements Location {
        private final double x, y, z;

        SimplePoint(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        @Override
        public double[] getCoordinates() {
            return new double[]{x, y, z};
        }

        @Override
        public double distanceTo(Location other) {
            double[] o = other.getCoordinates();
            if (o.length < 3) return Double.POSITIVE_INFINITY;
            double dx = x - o[0], dy = y - o[1], dz = z - o[2];
            return Math.sqrt(dx*dx + dy*dy + dz*dz);
        }

        @Override
        public boolean overlaps(Location other) {
            return distanceTo(other) < 0.001;
        }

        @Override
        public String toString() {
            return String.format("(%.2f, %.2f, %.2f)", x, y, z);
        }
    }

    private static class SimpleInterval implements TimeInterval {
        private final double start, end;

        SimpleInterval(double start, double end) {
            this.start = start;
            this.end = end;
        }

        @Override
        public double getStart() { return start; }

        @Override
        public double getEnd() { return end; }

        @Override
        public double getDuration() { return end - start; }

        @Override
        public boolean overlaps(TimeInterval other) {
            return this.start <= other.getEnd() && other.getStart() <= this.end;
        }

        @Override
        public boolean contains(double timestamp) {
            return start <= timestamp && timestamp <= end;
        }
    }

    public static class PhysicalObject extends Physical {
        private final String name;

        public PhysicalObject(EntityId id, Set<Property> properties, Location location,
                              TimeInterval timeInterval, String name) {
            super(id, properties, location, timeInterval);
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }
    }

    public enum SpatialRelationType {
        ADJACENT, ABOVE, BELOW, LEFT_OF, RIGHT_OF, IN_FRONT_OF, BEHIND, INSIDE, ON_TOP_OF
    }

    public static class SpatialRelation {
        private final Location from, to;
        private final SpatialRelationType type;

        public SpatialRelation(Location from, Location to, SpatialRelationType type) {
            this.from = from;
            this.to = to;
            this.type = type;
        }

        public Location getFrom() { return from; }
        public Location getTo() { return to; }
        public SpatialRelationType getType() { return type; }
    }

    public static class PhysicalWorldModel {
        private final List<Physical> physicalEntities;
        private final List<SpatialRelation> spatialRelations;
        private final Map<String, Object> globalProperties;

        public PhysicalWorldModel(List<Physical> physicalEntities,
                                   List<SpatialRelation> spatialRelations,
                                   Map<String, Object> globalProperties) {
            this.physicalEntities = Collections.unmodifiableList(new ArrayList<>(physicalEntities));
            this.spatialRelations = Collections.unmodifiableList(new ArrayList<>(spatialRelations));
            this.globalProperties = Collections.unmodifiableMap(new HashMap<>(globalProperties));
        }

        public List<Physical> getPhysicalEntities() { return physicalEntities; }
        public List<SpatialRelation> getSpatialRelations() { return spatialRelations; }
        public Map<String, Object> getGlobalProperties() { return globalProperties; }

        public List<Physical> findAtLocation(Location location) {
            return physicalEntities.stream()
                .filter(e -> e.getLocation().overlaps(location))
                .toList();
        }

        @Override
        public String toString() {
            return "PhysicalWorldModel{entities=" + physicalEntities.size() + "}";
        }
    }
}
