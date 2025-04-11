import java.util.*;

public class CampaignExecutionSystem {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        String firstLine = scanner.nextLine().trim();
        String[] parts = firstLine.split("\\s+");
        
        int campaignCount = Integer.parseInt(parts[0]);
        int coreCount = Integer.parseInt(parts[1]);
        
        List<Campaign> campaigns = new ArrayList<>();
        Set<String> campaignNames = new HashSet<>();
        
        // Parse campaigns
        for (int i = 0; i < campaignCount; i++) {
            String line = scanner.nextLine().trim();
            String[] campaignParts = line.split("\\s+");
            
            String name = campaignParts[0];
            int events = Integer.parseInt(campaignParts[1]);
            String priority = campaignParts.length > 2 ? campaignParts[2] : null;
            
            // Check for duplicate campaign names (case-insensitive)
            if (campaignNames.contains(name.toLowerCase())) {
                System.out.println("InvalidCampaigns");
                return;
            }
            campaignNames.add(name.toLowerCase());
            
            campaigns.add(new Campaign(name, events, priority));
        }
        
        int totalTime = calculateExecutionTime(campaigns, coreCount);
        System.out.println(totalTime);
    }
    
    private static int calculateExecutionTime(List<Campaign> campaigns, int coreCount) {
        int totalWorkers = 2 * coreCount;
        int totalTime = 0;
        
        // Group campaigns by priority
        Map<Integer, List<Campaign>> priorityGroups = new TreeMap<>();
        List<Campaign> standardCampaigns = new ArrayList<>();
        
        for (Campaign campaign : campaigns) {
            if (campaign.isPremium()) {
                int priorityValue = campaign.getPriorityValue();
                priorityGroups.putIfAbsent(priorityValue, new ArrayList<>());
                priorityGroups.get(priorityValue).add(campaign);
            } else {
                standardCampaigns.add(campaign);
            }
        }
        
        // Process premium campaigns by priority
        for (Map.Entry<Integer, List<Campaign>> entry : priorityGroups.entrySet()) {
            List<Campaign> samePriorityCampaigns = entry.getValue();
            
            // Sort by name for campaigns with same priority (lexicographically)
            Collections.sort(samePriorityCampaigns, Comparator.comparing(Campaign::getName, String.CASE_INSENSITIVE_ORDER));
            
            int campaignCount = samePriorityCampaigns.size();
            int workersPerCampaign = totalWorkers / campaignCount;
            
            if (workersPerCampaign == 0) {
                // If there are more campaigns than workers, process them in sequence
                int totalEvents = 0;
                for (Campaign campaign : samePriorityCampaigns) {
                    totalEvents += campaign.getEvents();
                }
                totalTime += (int) Math.ceil((double) totalEvents / totalWorkers);
            } else {
                // Process in parallel with equal worker distribution
                int maxPriorityTime = 0;
                for (Campaign campaign : samePriorityCampaigns) {
                    int timeNeeded = (int) Math.ceil((double) campaign.getEvents() / workersPerCampaign);
                    maxPriorityTime = Math.max(maxPriorityTime, timeNeeded);
                }
                totalTime += maxPriorityTime;
            }
        }
        
        // Process standard campaigns (one worker each)
        if (!standardCampaigns.isEmpty()) {
            // Sort standard campaigns by name
            Collections.sort(standardCampaigns, Comparator.comparing(Campaign::getName, String.CASE_INSENSITIVE_ORDER));
            
            // Use all available workers
            PriorityQueue<Integer> workerEndTimes = new PriorityQueue<>();
            for (int i = 0; i < totalWorkers; i++) {
                workerEndTimes.add(0);
            }
            
            for (Campaign campaign : standardCampaigns) {
                int earliestAvailableTime = workerEndTimes.poll();
                int newEndTime = earliestAvailableTime + campaign.getEvents();
                workerEndTimes.add(newEndTime);
            }
            
            int maxEndTime = 0;
            for (int time : workerEndTimes) {
                maxEndTime = Math.max(maxEndTime, time);
            }
            
            totalTime += maxEndTime;
        }
        
        return totalTime;
    }
    
    static class Campaign {
        private String name;
        private int events;
        private String priority;
        
        public Campaign(String name, int events, String priority) {
            this.name = name;
            this.events = events;
            this.priority = priority;
        }
        
        public String getName() {
            return name;
        }
        
        public int getEvents() {
            return events;
        }
        
        public boolean isPremium() {
            return priority != null && priority.startsWith("P");
        }
        
        public int getPriorityValue() {
            if (!isPremium()) return Integer.MAX_VALUE;
            return Integer.parseInt(priority.substring(1));
        }
    }
}
