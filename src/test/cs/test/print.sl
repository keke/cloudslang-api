namespace: test

operation:
  name: print

  inputs:
    - text

  action:
    python_script: print text;

  results:
    - SUCCESS
