package noe.common

import groovy.transform.TypeChecked
import noe.common.utils.Library

/**
 * Class for resolving current context and allowing to do different checks whether current context matches desired criteria.
 *
 * The context is provided usually via CONTEXT env variable.
 *
 * The provided context needs to be provided as String where each context is separated by one of these characters {',', '-', '+'},
 * where the ',' has special meaning of separating the contexts to groups. This is useful when user wants to define relations between contexts.
 * For example we want to define context, where EAP should be installed from zip and ews from RPMs,
 * this can be achieved by defining context like this 'CONTEXT=eap,ews+rpm', Thanks to grouping it is possible to do matching based only on group
 * and thus differentiate from which distribution channel the product should be taken (one from zip, another from rpm)
 *
 * If the context would be 'eap+ews+rpm' it would mean everything is installed from RPMs as everything is in single group.
 */
@TypeChecked
class NoeContext {

  private static final String CONTEXT_GROUP_SEPARATORS = ','
  private static final String CONTEXT_MODIFIERS_SEPARATORS = '+-'

  private static String contextAsString
  private static NoeContext instance

  private final List<Set<String>> contextsInGroups

  private NoeContext(List<Set<String>> contexts) {
    this.contextsInGroups = contexts
  }

  private NoeContext(String context) {
    def contexts = new ArrayList<Set<String>>()
    def contextGroups = context.tokenize(CONTEXT_GROUP_SEPARATORS)
    contextGroups.each { contextGroup ->
      def contextGroupItems = contextGroup.tokenize(CONTEXT_MODIFIERS_SEPARATORS)
      if (!contextGroupItems.isEmpty()) {
        contexts.add(new HashSet<String>(contextGroupItems))
      }
    }
    this.contextsInGroups = contexts
  }

  /**
   * Creates lazily instance of NoeContext, if instance for provided context (in String format) is the same as last one created, the last one is just returned,
   * if for given context it doesn't exist, new instance is created.
   * @param context String containing list of contexts separated by '+' or '-' or ',' where ',' is used also as context groups separator
   * @return instance of NoeContext
   */
  public static NoeContext forContext(final String context) {
    if (instance == null || contextAsString != context) {
      instance = new NoeContext(context)
      contextAsString = context
    }
    return instance
  }

  /**
   * Returns instance of context based on value retrieved by {@code Library.getUniversalProperty(context)}
   * For info how the actual parsing is done  {@see NoeContext.forContext(String)}
   */
  public static NoeContext forCurrentContext() {
    final String context = Library.getUniversalProperty("context", DefaultProperties.DEFAULT_CONTEXT_NAME)
    if (instance == null || contextAsString != context) {
      instance = new NoeContext(context)
      contextAsString = context
    }
    return instance
  }

  /**
   * Returns true if any of the provided contexts matches any of defined contexts in current instance of NoeContext, false otherwise.
   * It basically mimics OR behaviour
   *
   * It ignores groups
   */
  boolean consistsOfAny(List<String> contexts) {
    return !contextsInGroups.flatten().intersect(contexts).isEmpty()
  }

  /**
   * Returns true if all of the provided contexts matches any of defined contexts in current instance of NoeContext, false otherwise.
   * It basically mimics AND behaviour
   *
   * It ignores groups
   */
  boolean consistsOf(List<String> contexts) {
    return contextsInGroups.flatten().containsAll(contexts)
  }

  /**
   * Returns true if all of the provided contexts matches at least subset of contexts in single group, false otherwise.
   */
  boolean areInSingleGroup(List<String> contexts) {
    contextsInGroups.any {
      it.containsAll(contexts)
    }
  }


  @Override
  public String toString() {
    return contextAsString
  }
}
