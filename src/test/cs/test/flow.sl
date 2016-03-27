namespace: test

flow:
  name: flow

  workflow:
    - sayHi:
        do:
          print:
            - text: "Hello Python script"