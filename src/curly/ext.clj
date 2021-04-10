(ns curly.ext)

(defmulti munge-arguments
  "Modify the raw command line arguments before they are applied to the command
  map."
  #(first %&) :default :curly/default)

(defmethod munge-arguments :curly/default
  [_command-key args]
  args)

(defmulti munge-command
  "Modify the command map after arguments have been applied."
  #(first %&) :default :curly/default)

(defmethod munge-command :curly/default
  [_command-key command]
  command)

(defmulti munge-curl-command
  "Modify the curl command array right before running."
  #(first %&) :default :curly/default)

(defmethod munge-curl-command :curly/default
  [_command-key curl-command]
  curl-command)
