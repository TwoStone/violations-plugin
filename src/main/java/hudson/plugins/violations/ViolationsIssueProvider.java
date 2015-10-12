package hudson.plugins.violations;

import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.Action;
import hudson.plugins.analysis.util.ContextHashCode;
import hudson.plugins.violations.hudson.AbstractViolationsBuildAction;
import hudson.plugins.violations.model.FileModel;
import hudson.plugins.violations.model.Violation;
import hudson.plugins.violations.render.FileModelProxy;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.jenkinsci.plugins.codehealth.provider.issues.Issue;
import org.jenkinsci.plugins.codehealth.provider.issues.IssueProvider;
import org.jenkinsci.plugins.codehealth.model.Priority;

import java.io.IOException;
import java.util.*;

/**
 * IssueProvider which maps Violations to Issues.
 *
 * @author Michael Prankl
 */
@Extension
public class ViolationsIssueProvider extends IssueProvider {

    private ContextHashCode contextHasher = new ContextHashCode();

    @Override
    public Collection<Issue> getExistingIssues(AbstractBuild<?, ?> build) {
        final Collection<Issue> issues = new ArrayList<Issue>();
        AbstractViolationsBuildAction violationAction = getViolationsBuildAction(build);
        if (violationAction != null) {
            ViolationsReport report = violationAction.findReport();
            Map<String, FileModelProxy> fileModelMap = report.getModel().getFileModelMap();
            for (Map.Entry<String, FileModelProxy> fileModelEntry : fileModelMap.entrySet()) {
                String type = fileModelEntry.getKey();
                FileModelProxy fileModelProxy = fileModelEntry.getValue();
                FileModel fileModel = fileModelProxy.getFileModel();
                TreeMap<String, TreeSet<Violation>> typeMap = fileModel.getTypeMap();
                for (TreeSet<Violation> violations : typeMap.values()) {
                    for (Violation violation : violations) {
                        Priority priority = mapPriority(violation);
                        String absolutePath = fileModel.getSourceFile().getAbsolutePath();
                        try {
                            Issue i = new Issue(computeContextHashCode(absolutePath, violation), violation.getMessage(), priority);
                            issues.add(i);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            return issues;
        } else {
            return null;
        }
    }

    private Priority mapPriority(Violation violation) {
        Priority prio = null;
        if (violation.getSeverityLevel() == 0) {
            prio = Priority.HIGH;
        } else if (violation.getSeverityLevel() > 0 && violation.getSeverityLevel() <= 2) {
            prio = Priority.NORMAL;
        } else {
            prio = Priority.LOW;
        }
        return prio;
    }

    private int computeContextHashCode(String absoluteFilePath, Violation violation) throws IOException {
        HashCodeBuilder builder = new HashCodeBuilder();
        int contextHash = contextHasher.create(absoluteFilePath, violation.getLine(), null);
        builder.append(contextHash);
        builder.append(violation.getType());
        builder.append(violation.getSource());
        builder.append(violation.getSeverityLevel());
        return builder.toHashCode();
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
        return MagicNames.VIOLATIONS;
    }

    @Override
    public boolean canProvideFixedIssues() {
        return false;
    }
}
