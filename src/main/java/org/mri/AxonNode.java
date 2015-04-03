package org.mri;

import com.google.common.collect.Lists;
import spoon.reflect.reference.CtExecutableReference;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.CaseFormat.LOWER_CAMEL;
import static com.google.common.base.CaseFormat.LOWER_HYPHEN;

public class AxonNode {

    enum Type {
        CONTROLLER, COMMAND, COMMAND_HANDLER, EVENT, EVENT_LISTENER, AGGREGATE;
    }

    private final Type type;
    private final CtExecutableReference reference;
    private List<AxonNode> children = new ArrayList<>();

    public AxonNode(Type type, CtExecutableReference reference) {
        this.type = type;
        this.reference = reference;
    }

    public void add(AxonNode node) {
        this.children.add(node);
    }

    public boolean hasChildren() {
        return !this.children.isEmpty();
    }

    public CtExecutableReference reference() {
        return reference;
    }

    public void print(PrintStream printStream) {
        if (children.isEmpty()) {
            return;
        }

        print(printStream, "");
    }

    private void print(PrintStream printStream, String indent) {
        printStream.println(indent + "<<" + type + ">>");
        printStream.println(indent + reference.toString());
        for (AxonNode node : children) {
            node.print(printStream, indent.concat("\t"));
        }
    }

    public void printPlantUML(PrintStream printStream) {
        if (children.isEmpty()) {
            return;
        }

        printStream.println("@startuml " + LOWER_CAMEL.to(LOWER_HYPHEN, reference.getSimpleName()) + "-flow.png");
        List<AxonNode> all = Lists.newArrayList(this);
        all.addAll(descendants());
        for (AxonNode each : all) {
            printStream.println("participant \"" + prettyActorName(each.reference) + "\" as " + actorName(each.reference));
        }
        printStream.println();
        printPlantUMLComponent(printStream);
        printStream.println("@enduml");
    }

    private List<AxonNode> descendants() {
        List<AxonNode> all = new ArrayList<>();
        for (AxonNode child : children) {
            all.add(child);
            all.addAll(child.descendants());
        }
        return all;
    }

    private void printPlantUMLComponent(PrintStream printStream) {
        for (AxonNode child : children) {
            printStream.println(
                    actorName(this.reference())
                            + " "
                            + transition()
                            + " "
                            + actorName(child.reference())
                            + ": "
                            + methodName(child));
            child.printPlantUMLComponent(printStream);
        }
    }

    private String prettyActorName(CtExecutableReference reference) {
        return "**" + reference.getDeclaringType().getSimpleName() + "**"
                        + "\\n//" + reference.getSimpleName() + "//";
    }

    private String actorName(CtExecutableReference reference) {
        String methodName = reference.getSimpleName();
        if (methodName.equals("<init>")) {
            methodName = "ctr";
        }
        return reference.getDeclaringType().getSimpleName() + "." + methodName;
    }

    private String transition() {
        switch (type) {
            case CONTROLLER:
            case COMMAND_HANDLER:
            case EVENT_LISTENER:
            case AGGREGATE:
                return "->";
            case COMMAND:
            case EVENT:
                return "-->";
        }
        return "->";
    }

    private String methodName(AxonNode child) {
        switch (type) {
            case CONTROLLER:
            case COMMAND_HANDLER:
            case EVENT_LISTENER:
                return "create";
            case COMMAND:
            case EVENT:
            case AGGREGATE:
                return child.reference.getSimpleName();
        }
        return "<<call>>";
    }
}
