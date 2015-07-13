package hudson.plugins.violations;

import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.Action;
import hudson.plugins.violations.hudson.AbstractViolationsBuildAction;
import hudson.plugins.violations.model.FileModel;
import hudson.plugins.violations.model.Violation;
import hudson.plugins.violations.render.FileModelProxy;
import org.jenkinsci.plugins.codehealth.Issue;
import org.jenkinsci.plugins.codehealth.IssueProvider;
import org.jenkinsci.plugins.codehealth.model.Priority;

import java.util.*;

/**
 * @author Michael Prankl
 */
@Extension
public class ViolationsIssueProvider extends IssueProvider {
    @Override
    public Collection<Issue> getExistingIssues(AbstractBuild<?, ?> build) {
        final Collection<Issue> issues = new ArrayList<Issue>();
        AbstractViolationsBuildAction violationAction = getViolationsBuildAction(build);
        ViolationsReport report = violationAction.findReport();
        Map<String, FileModelProxy> fileModelMap = report.getModel().getFileModelMap();
        for (Map.Entry<String, FileModelProxy> fileModelEntry : fileModelMap.entrySet()) {
            String type = fileModelEntry.getKey();
            FileModelProxy fileModelProxy = fileModelEntry.getValue();
            FileModel fileModel = fileModelProxy.getFileModel();
            TreeMap<String, TreeSet<Violation>> typeMap = fileModel.getTypeMap();
            for (TreeSet<Violation> violations : typeMap.values()) {
                for (Violation violation : violations) {
                    Priority prio = null;
                    if (violation.getSeverityLevel() == 0) {
                        prio = Priority.HIGH;
                    } else if (violation.getSeverityLevel() > 0 && violation.getSeverityLevel() <= 2) {
                        prio = Priority.NORMAL;
                    } else {
                        prio = Priority.LOW;
                    }
                    // TODO calculate contextHashCode
                    Issue i = new Issue(1L, violation.getMessage(), prio);
                    issues.add(i);
                }
            }
        }
        return issues;
    }

    private AbstractViolationsBuildAction getViolationsBuildAction(AbstractBuild<?, ?> build) {
        AbstractViolationsBuildAction violationAction = null;
        for (Action action : build.getPersistentActions()) {
            if (action instanceof AbstractViolationsBuildAction) {
                violationAction = (AbstractViolationsBuildAction) action;
                break;
            }
        }
        return violationAction;
    }

    @Override
    public Collection<Issue> getFixedIssues(AbstractBuild<?, ?> build) {
        return null;
    }

    @Override
    public String getOrigin() {
        return "violations";
    }

    @Override
    public boolean canProvideFixedIssues() {
        return false;
    }
}
