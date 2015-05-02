package com.jpexs.bomutils.ant;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;

/**
 *
 * @author JPEXS
 */
public abstract class Task {

    protected Project project;

    public void setProject(Project proj) {
        project = proj;
    }

    public abstract void execute() throws BuildException;
}
